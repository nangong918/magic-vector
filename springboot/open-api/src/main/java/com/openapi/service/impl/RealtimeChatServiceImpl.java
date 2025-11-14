package com.openapi.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSON;
import com.openapi.component.manager.OptimizedSentenceDetector;
import com.openapi.component.manager.mixLLM.MixLLMManager;
import com.openapi.component.manager.realTimeChat.RealtimeChatContextManager;
import com.openapi.config.AgentConfig;
import com.openapi.config.ChatConfig;
import com.openapi.config.ThreadPoolConfig;
import com.openapi.converter.ChatMessageConverter;
import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.constant.error.UserExceptions;
import com.openapi.domain.constant.realtime.RealtimeResponseDataTypeEnum;
import com.openapi.domain.dto.ws.response.RealtimeChatTextResponse;
import com.openapi.domain.exception.AppException;
import com.openapi.interfaces.OnSTTResultCallback;
import com.openapi.interfaces.mixLLM.LLMCallback;
import com.openapi.interfaces.mixLLM.TTSCallback;
import com.openapi.interfaces.model.TTSStateCallback;
import com.openapi.interfaces.model.StreamCallErrorCallback;
import com.openapi.service.AgentService;
import com.openapi.service.ChatMessageService;
import com.openapi.service.PromptService;
import com.openapi.service.RealtimeChatService;
import com.openapi.service.UserService;
import com.openapi.service.model.VLService;
import com.openapi.service.tools.VisionToolService;
import com.openapi.service.model.LLMServiceService;
import com.openapi.service.model.TTSServiceService;
import com.openapi.connect.websocket.manager.PersistentConnectMessageManager;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Subscription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author 13225
 * @date 2025/10/16 10:08
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeChatServiceImpl implements RealtimeChatService {

    private final ChatConfig chatConfig;
    private final OptimizedSentenceDetector optimizedSentenceDetector;
    private final Recognition sttRecognizer;
    private final ChatMessageService chatMessageService;
    private final ChatMessageConverter chatMessageConverter;
    private final AgentConfig agentConfig;
    private final UserService userService;
    private final AgentService agentService;
    private final ThreadPoolConfig threadPoolConfig;
    private final PromptService promptService;
    private final VisionToolService visionToolService;
    private final VLService visionChatService;
    private final PersistentConnectMessageManager webSocketMessageManager;
    private final TTSServiceService ttsServiceService;
    private final LLMServiceService llmServiceService;

    /**
     * 初始化ChatClient
     * 功能包括：1.绑定agentId，2.载入agent设定 3.载入历史记录
     * @param chatContextManager    chatContextManager
     * @param chatModel             chatModel
     * @return                      ChatClient
     * @throws AppException         AppException
     */
    @NotNull
    @Override
    public ChatClient initChatClient(@NotNull RealtimeChatContextManager chatContextManager, @NotNull DashScopeChatModel chatModel) throws AppException {
        if (!userService.checkUserExistById(chatContextManager.userId)){
            throw new AppException(UserExceptions.USER_NOT_EXIST);
        }
        AgentAo agentAo = agentService.getAgentById(chatContextManager.agentId);
        if (agentAo == null || agentAo.getAgentId() == null){
            throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
        }

        // 设定
        String description = Optional.ofNullable(agentAo.getAgentVo())
                .map(agentVo -> agentVo.description)
                .orElseGet(() -> {
                    log.warn("Agent 没有设定，使用默认设定");
                    return ModelConstant.SYSTEM_PROMPT;
                });

        ChatMemory chatMemory = agentConfig.chatMemory();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 此处的defaultSystem存在问题，傻逼openAI会将其视为用户的输入，需要设计提示词工程
                .defaultSystem(promptService.getSystemPrompt(description))
                .build();

        // 预先加载10条历史聊天记录
        List<ChatMessageDo> chatMessageDos = chatMessageService.getLast10Messages(chatContextManager.agentId);
        // 将历史消息添加到ChatMemory中
        if (!chatMessageDos.isEmpty()) {

            // 按时间正序排列，确保对话顺序正确 （前端展示是最新的放在第0个，而此处是最新的放在最后一个添加，所以需要重排序）
            List<ChatMessageDo> sortedMessages = chatMessageDos.stream()
                    .sorted(Comparator.comparing(ChatMessageDo::getChatTime))
                    .toList();

            List<Message> historyMessages = chatMessageConverter.chatMessageDoListToMessageList(sortedMessages);
            for (Message message : historyMessages) {
                chatMemory.add(chatContextManager.agentId, message);
            }
        }

        return chatClient;
    }

    /**
     * 开始文本聊天
     * @param userQuestion          userQuestion
     * @param chatContextManager    chatContextManager
     * @throws AppException         Agent相关的AppException
     */
    @Override
    public void startTextChat(@NotNull String userQuestion, @NotNull RealtimeChatContextManager chatContextManager) throws AppException {
        log.info("[startTextChat] 开始文本聊天：userQuestion={}", userQuestion);

        // 前端传递过来再传递回去是因为需要分配messageId
        RealtimeChatTextResponse userAudioSttResponse = chatContextManager.getUserTextResponse(userQuestion);
        String response = JSON.toJSONString(userAudioSttResponse);

        // 保存到数据库
        ChatMessageDo chatMessageDo = null;
        try {
            chatMessageDo = chatMessageConverter.realtimeChatTextResponseToChatMessageDo(userAudioSttResponse, userAudioSttResponse.chatTime);
            String messageId = chatMessageService.insertOne(chatMessageDo);
            log.info("保存用户TextChat数据到数据库：{}", messageId);
        } catch (Exception e) {
            log.error("保存用户TextChat数据到数据库异常", e);
        }

        // 发送给user的text给Client
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
        responseMap.put(RealtimeResponseDataTypeEnum.DATA, response);
        String userTextResponse = JSON.toJSONString(responseMap);
        webSocketMessageManager.submitMessage(
                chatContextManager.agentId,
                userTextResponse
        );

        /// LLM
        String llmResult = llmServiceService.mixLLMCallErrorProxy(
                userQuestion,
                chatContextManager.chatClient,
                chatContextManager.agentId,
                chatContextManager.getCurrentContextParam(),
                chatContextManager.mcpSwitch,
                new StreamCallErrorCallback() {
                    @Override
                    public int @NonNull [] addCountAndCheckIsOverLimit() {
                        return chatContextManager.addCountAndCheckIsOverLimit();
                    }

                    @Override
                    public void addTask(Object task) {
                        chatContextManager.addChatTask(task);
                    }

                    @Override
                    public void endConversation() {
                        chatContextManager.endConversation();
                    }
                }
        );

        TTSCallback ttsCallback = MixLLMManager.getDefaultTTSCallback(
                chatContextManager,
                webSocketMessageManager
        );

        LLMCallback llmCallback = MixLLMManager.getDefaultLLMCallback(
                chatContextManager,
                webSocketMessageManager
        );

        /// TTS
        chatContextManager.mixLLMManager.start(
                llmResult,
                ttsServiceService,
                ttsCallback,
                llmCallback
        );
    }

    /**
     * 启动音频聊天
     * 内部有等待音频录制结束的死循环，上游需要避免main线程被阻塞
     * @param chatContextManager        聊天上下文管理器
     * @throws InterruptedException     线程中断异常
     */
    @Override
    public void startAudioChat(@NotNull RealtimeChatContextManager chatContextManager) throws InterruptedException {
        log.info("[startAudioChat] 开始将音频流数据填充缓冲区");

        var chatClient = chatContextManager.chatClient;
        if (chatClient == null){
            chatContextManager.endConversation();
            throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
        }

        // 开始记录 + 设置是音频聊天
        chatContextManager.startRecord();

        /// audioBytes
        ByteArrayOutputStream audioOutStream = new ByteArrayOutputStream();

        var sttRecordContext = chatContextManager.llmProxyContext.getSttRecordContext();
        // 循环等待直到录音结束，该循环会阻塞，所以上游需要使用线程池提交任务
        while (sttRecordContext.isRecording()) {
            // 从队列中获取数据
            byte[] audioData = sttRecordContext.pollAudioBuffer();

            if (audioData != null) {
                // 将数据写入输出流
                int length = audioData.length;
                if (length > 0) {
                    audioOutStream.write(audioData, 0, length);
                }
            }
            else {
                // 10 毫秒休眠
                Thread.sleep(10);
            }
        }

        // 获取音频识别结果回调
        var onSSTResultCallback = getOnSTTResultCallback(chatContextManager);

        // 录制音频结束之后的处理
        handleOnAudioRecordFinish(chatContextManager, audioOutStream, onSSTResultCallback);
    }

    /**
     * 音频数据处理
     * @param chatContextManager    聊天上下文管理器
     * @param audioOutStream        音频输出流
     */
    private void handleOnAudioRecordFinish(
            @NotNull RealtimeChatContextManager chatContextManager,
            @NotNull ByteArrayOutputStream audioOutStream,
            @NotNull OnSTTResultCallback onSSTResultCallback){
        // 发送录音数据
        byte[] audioData = audioOutStream.toByteArray();

        if (audioData.length == 0){
            log.warn("[startAudioChat] 录音数据为空");
            chatContextManager.endConversation();
            return;
        }

        /// stt
        Flowable<ByteBuffer> audioFlowable = convertAudioToFlowable(audioData);

        // 管理异步调用音频byte[]转换为ByteBuffer的方法
        var audioToByteBufferDisposable = audioFlowable
                // 判断 audioFlowable 是否为空
                .isEmpty()
                .subscribe(isEmpty -> {
                    if (!isEmpty) {
                        // 只有在 audioFlowable 不为空时才调用 sttStreamCall
                        var sttDisposable = sttStreamCall(audioFlowable, onSSTResultCallback);
                        // STT转换的Disposable存储
                        chatContextManager.addChatTask(sttDisposable);
                    }
                    else {
                        log.warn("[STT] audioFlowable是empty，不做处理");
                    }
                });
        // 音频byte转换的Disposable存储
        chatContextManager.addChatTask(audioToByteBufferDisposable);
    }

    private OnSTTResultCallback getOnSTTResultCallback(RealtimeChatContextManager chatContextManager) {
        return sttResult -> {
            // 返回结果给前端
            RealtimeChatTextResponse userAudioSttResponse = chatContextManager.getUserSTTResultResponse(sttResult);
            String response = JSON.toJSONString(userAudioSttResponse);

            // 保存到数据库
            ChatMessageDo chatMessageDo = null;
            try {
                chatMessageDo = chatMessageConverter.realtimeChatTextResponseToChatMessageDo(userAudioSttResponse, userAudioSttResponse.chatTime);
                String messageId = chatMessageService.insertOne(chatMessageDo);
                log.info("保存用户语音识别结果到数据库：{}", messageId);
            } catch (Exception e) {
                log.error("保存用户语音识别结果到数据库失败：", e);
            }

            // 发送给Client
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
            responseMap.put(RealtimeResponseDataTypeEnum.DATA, response);

            String startResponse = JSON.toJSONString(responseMap);

            webSocketMessageManager.submitMessage(
                    chatContextManager.agentId,
                    startResponse
            );

            if (!StringUtils.hasText(sttResult)) {
                log.warn("[startAudioChat] 语音识别结果为空");
                return;
            }

            /// LLM
            String llmResult = llmServiceService.mixLLMCallErrorProxy(
                    sttResult,
                    chatContextManager.chatClient,
                    chatContextManager.agentId,
                    chatContextManager.getCurrentContextParam(),
                    chatContextManager.mcpSwitch,
                    new StreamCallErrorCallback() {
                        @Override
                        public int @NonNull [] addCountAndCheckIsOverLimit() {
                            return chatContextManager.addCountAndCheckIsOverLimit();
                        }

                        @Override
                        public void addTask(Object task) {
                            chatContextManager.addChatTask(task);
                        }

                        @Override
                        public void endConversation() {
                            chatContextManager.endConversation();
                        }
                    },
                    // MCP Tools
                    visionToolService
            );

            /// TTS + Event
            chatContextManager.mixLLMManager.start(
                    llmResult,
                    ttsServiceService,
                    MixLLMManager.getDefaultTTSCallback(chatContextManager, webSocketMessageManager),
                    MixLLMManager.getDefaultLLMCallback(chatContextManager, webSocketMessageManager)
            );
//                var llmDisposable = toolsLLMStreamCall(result, chatContextManager);
//                chatContextManager.addChatTask(llmDisposable);
        };
    }

    private static Flowable<ByteBuffer> convertAudioToFlowable(byte[] audioData) {
        return Flowable.create(emitter -> {
            try {
                if (audioData != null && audioData.length > 0) {
                    // 直接使用传入的音频数据，创建 ByteBuffer 并发送
                    ByteBuffer byteBuffer = ByteBuffer.wrap(audioData);
                    emitter.onNext(byteBuffer);
                    emitter.onComplete();
                }
                else {
                    log.warn("音频数据为空");
                    emitter.onError(new IllegalArgumentException("音频数据为空"));
                }
            } catch (Exception e) {
                log.error("[convertAudioToFlowable] 转换音频数据失败", e);
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }

    /**
     * 语音识别STT任务
     * @param audioSource           音频数据
     * @param callback              回调
     * @return                      用于取消STT任务的Disposable
     * @throws NoApiKeyException    无ApiKey异常
     */
    private io.reactivex.disposables.Disposable sttStreamCall(Flowable<ByteBuffer> audioSource, OnSTTResultCallback callback) throws NoApiKeyException {
        // 创建RecognitionParam，audioFrames参数中传入上面创建的Flowable<ByteBuffer>
        RecognitionParam sttParam = RecognitionParam.builder()
                .model(ModelConstant.STT_Model)
                .format(ModelConstant.STT_Format)
                .sampleRate(ModelConstant.SST_SampleRate)
                .apiKey(chatConfig.getApiKey())
                .build();

        return sttRecognizer.streamCall(sttParam, audioSource)
                .subscribe(
                        result -> {
                            // 打印最终结果
                            if (result.isSentenceEnd()) {
                                String sentence = result.getSentence().getText();
                                callback.onResult(sentence);
                                log.info("[sttStreamCall] 识别结果: {}", sentence);
                            }
                        },
                        throwable -> {
                            // 错误处理
                            log.error("[sttStreamCall] 发生错误: {}", throwable.getMessage());
                        },
                        () -> {
                            // 完成处理
                            log.info("[sttStreamCall] 识别完成");
                        }
                );
    }


/*
    private reactor.core.Disposable toolsLLMStreamCall(String sentence, @NotNull RealtimeChatContextManager chatContextManager){
        var callback = new LLMStateCallback() {
            final StringBuffer textBuffer = new StringBuffer();
            @Override
            public void onStart(Subscription subscription) {
                // 设置Agent message Time
                chatContextManager.currentAgentMessageTimestamp = System.currentTimeMillis();

                // 发送开始标识
                Map<String, String> responseMap = new HashMap<>();
                responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.START_TTS.getType());
                responseMap.put(RealtimeResponseDataTypeEnum.DATA, RealtimeResponseDataTypeEnum.START_TTS.getType());
                String startResponse = JSON.toJSONString(responseMap);
                webSocketMessageManager.submitMessage(
                        chatContextManager.agentId,
                        startResponse
                );

                // 设置是首次TTS
                chatContextManager.setIsFirstTTS(true);

                // 开始llm
                chatContextManager.startLLM();

                log.info("[toolsLLMStreamCall] LLM-Tools开始");
            }

            @Override
            public void onFinish(SignalType signalType) {
                // 检查是否有剩余的
                String remainingText = optimizedSentenceDetector.extractAllCompleteSentences(textBuffer);
                if (StringUtils.hasText(remainingText)) {
                    log.info("[toolsLLMStreamCall] LLM -> TTS 剩余: {}", remainingText);
                    // 添加到TTS MQ
                    chatContextManager.llmProxyContext.offerTTS(remainingText);
                    log.info("[toolsLLMStreamCall] 剩余填充后的TTS MQ大小：{}", chatContextManager.llmProxyContext.getAllTTSCount());

                    if (!chatContextManager.isTTSing()){
                        log.warn("[toolsLLMStreamCall] TTS结束了 并且还存在数据，数量：{}, 重新调用TTS", chatContextManager.llmProxyContext.getAllTTSCount());
                    }
                }
                chatContextManager.stopLLM();
                log.info("[toolsLLMStreamCall] LLM-Tools结束");
            }

            @Override
            public void onNext(String fragment) {
                System.out.println("tool fragment = " + fragment);

                // 发送当前fragment消息
                chatContextManager.agentResponseStringBuffer.append(fragment);
                // 添加数据到缓存textBuffer
                textBuffer.append(fragment);

                RealtimeChatTextResponse agentFragmentResponse = chatContextManager.getCurrentFragmentAgentResponse(fragment);

                // 发送消息给Client
                String agentFragmentResponseJson = JSON.toJSONString(agentFragmentResponse);
                Map<String, String> fragmentResponseMap = new HashMap<>();
                fragmentResponseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
                fragmentResponseMap.put(RealtimeResponseDataTypeEnum.DATA, agentFragmentResponseJson);
                String response = JSON.toJSONString(fragmentResponseMap);
                webSocketMessageManager.submitMessage(
                        chatContextManager.agentId,
                        response
                );

                if (chatContextManager.llmProxyContext.isFirstTTS().get()){
                    String complete1Sentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer);

                    if (StringUtils.hasText(complete1Sentence)){
                        // 添加待处理的TTS句子到TTS MQ
                        chatContextManager.llmProxyContext.offerTTS(complete1Sentence);
                        // 此处如果不提交线程池，可能会阻塞LLM
                        // 改进之后的TTS，只在LLM中调用一次
                        var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                () -> {
                                    log.info("[toolsLLMStreamCall] LLM-Tools 首次TTS");
                                    var chatTTSDisposable = proxyGenerateAudio(
                                            chatContextManager,
                                            false
                                    );
                                    chatContextManager.addChatTask(chatTTSDisposable);
                                }
                        );
                        chatContextManager.addChatTask(ttsFuture);

                        // 设置不是首次TTS
                        chatContextManager.setIsFirstTTS(false);
                    }
                }
                else {
                    // 尝试从缓冲区提取2个完整句子并输出
                    String complete2Sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 2);
                    if (StringUtils.hasText(complete2Sentence)){
                        chatContextManager.llmProxyContext.offerTTS(complete2Sentence);
                    }
                }

                // 显示当前缓冲区剩余内容
                if (!textBuffer.isEmpty()) {
                    System.out.println("[[LLMStreamCall] LLM-Tools 缓冲区剩余]: " + textBuffer);
                }
            }

            @Override
            public void haveNoSentence() {
                log.warn("[toolsLLMStreamCall] LLM-Tools没有完整句子");
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("[toolsLLMStreamCall] LLM-Tools 错误", throwable);

                // (待优化A1)长时间未连接之后再次连接会发生Connect Reset目前采用的是递归的方式重试，可能造成堆栈溢出。考虑改为循环调用的方式
                // 尝试再次自我调用
                if (throwable instanceof WebClientRequestException || throwable instanceof TimeoutException){
                    log.error("[toolsLLMStreamCall] LLM-Tools 尝试再次自我调用；错误信息, 错误类型: ", throwable);
//                        // 再次自调用
                    if (chatContextManager.getLLMErrorCount() < ModelConstant.LLM_CONNECT_RESET_MAX_RETRY_COUNT){
                        int attempt = chatContextManager.llmProxyContext.getLLMConnectResetRetryCount().incrementAndGet();
                        log.warn("[toolsLLMStreamCall] LLM-Tools 警告，检测到连接重置，进行第{}次重试", attempt);

                        var disposable = toolsLLMStreamCall(sentence, chatContextManager);
                        chatContextManager.addChatTask(disposable);
                    }
                    else {
                        log.error("[toolsLLMStreamCall] LLM-Tools 连接重置次数过多，已尝试{}次，放弃重试", chatContextManager.getLLMErrorCount());
                        chatContextManager.endConversation();
                    }
                }
                else {
                    log.error("[toolsLLMStreamCall] LLM-Tools 错误信息, 并非WebClientRequestException问题", throwable);
                    chatContextManager.endConversation();
                }

                // 重置重连次数
                chatContextManager.llmProxyContext.getLLMConnectResetRetryCount().set(0);
            }
        };
        return llmServiceService.LLMStreamCall(
                sentence,
                chatContextManager.chatClient,
                chatContextManager.agentId,
                chatContextManager.getCurrentContextParam(),
                callback,
                visionToolService
        );
    }

*/

    /**
     * 设计模式：代理模式：解耦generateAudio的功能；
     * <p>
     *      generateAudio就应只管理生成Audio;
     * <p>
     *      commonGenerateAudio只管理整个TTS是否完成
     * <p>
     *      proxyGenerateAudio管理区别FunctionCall和普通任务
     *
     * @param chatContextManager        聊天上下文管理器
     * @param isFunctionCall            是否是FunctionCall任务
     * @return                          线程订阅管理
     */
/*
    private io.reactivex.disposables.Disposable proxyGenerateAudio(
            @NotNull RealtimeChatContextManager chatContextManager,
            boolean isFunctionCall
    ) {
        try {
            return commonGenerateAudio(chatContextManager, new TTSStateCallback() {
                int count = 0;

                @Override
                public void recordDisposable(Disposable disposable) {

                }

                @Override
                public void onStart(Subscription subscription) {

                }

                @Override
                public void onSingleComplete() {
                    count++;
                    log.info("[proxy TTS] 单次TTS结束, count: {}", count);
                }

                @Override
                public void onAllComplete() {
                    boolean finalIsFunctionCall = chatContextManager.llmProxyContext.isFunctionCall() || isFunctionCall;
                    // 是否是最后一个tts
                    boolean isFinalResultTTs = chatContextManager.isFinalResultTTS();

                    // 如果不是Function Call，直接发送EOF
                    if (!finalIsFunctionCall){
                        log.info("[proxy TTS]结束，不是FunctionCall任务，直接发送EOF");
                        chatContextManager.endConversation();
                    }
                    // 是FunctionCall任务，并且是最后一个tts
                    else if (finalIsFunctionCall && isFinalResultTTs){
                        log.info("[proxy TTS]结束 是FunctionCall任务，是最终的TTS，直接发送EOF; " +
                                "isFunctionCalling: {}, isFunctionCall: {}", chatContextManager.llmProxyContext.isFunctionCall(), isFunctionCall);
                        chatContextManager.endConversation();
                    }
                    // 无视编译器警告，因为编译器是傻逼，根本就不考虑可扩展性
                    else if (finalIsFunctionCall && !isFinalResultTTs) {
                        log.info("[proxy TTS]结束 是FunctionCall任务，是首次TTS，无需无需考虑");
                    }

                    log.info("[proxy TTS] 全部结束");
                }

                @Override
                public void onNext(String audioBase64Data) {

                }

                @Override
                public void haveNoSentence() {
                    log.warn("[proxy TTS] 没有待转换的句子");
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("[proxy TTS] 错误", throwable);
                }
            });
        } catch (NoApiKeyException | UploadFileException e) {
            log.error("[proxy TTS] 错误", e);
            chatContextManager.endConversation();
            return null;
        }
    }
*/


/*    @Nullable
    private io.reactivex.disposables.Disposable commonGenerateAudio(
            @NotNull RealtimeChatContextManager chatContextManager,
            @NotNull TTSStateCallback additionalProxyCallback
            ) throws NoApiKeyException, UploadFileException {
        if (chatContextManager.isTTSing()){
            log.info("[common TTS] 正在生成音频，请稍等...");
            return null;
        }

        // 此处存在休眠，上游需要考虑是否交给线程池
        long ttsGapTime = System.currentTimeMillis() - chatContextManager.getTTSStartTime();
        if (ttsGapTime < ModelConstant.SENTENCE_INTERVAL){
            try {
                log.info("[common TTS] 模拟人语音停顿 暂停 {}ms", ModelConstant.SENTENCE_INTERVAL - ttsGapTime);
                Thread.sleep(ModelConstant.SENTENCE_INTERVAL - ttsGapTime);
            } catch (InterruptedException e) {
                // 此处只打印e.message不打印堆栈是因为本来就是要用Disposable来让线程中断，此处并不视为一个报错。所以日志也是info级别
                log.info("[common TTS] 次线程任务被取消，休眠线程中断: {}", e.getMessage());
            }
        }
        else {
            log.info("[common TTS] 无需模拟人语音停顿, 距离上次TTS已经耗时 {}ms", ttsGapTime);
        }

        // 全部取出
        String sentence = chatContextManager.llmProxyContext.getAllTTS();

        if (sentence.isEmpty()){
            additionalProxyCallback.haveNoSentence();
            log.info("[common TTS] 无需生成音频，句子为空");
            return null;
        }

        return ttsServiceService.generateAudio(sentence, new TTSStateCallback() {
            @Override
            public void recordDisposable(Disposable disposable) {

            }

            @Override
            public void onStart(Subscription subscription) {
                chatContextManager.startTTS();
                log.info("[common TTS] 调用开始, sentence: {}", sentence);
                // 回调上游代理
                additionalProxyCallback.onStart(subscription);
            }

            @Override
            public void onSingleComplete() throws NoApiKeyException, UploadFileException {
                // 记录结束时间
                chatContextManager.setTTSStartTime(System.currentTimeMillis());

                // 自我调用检查 [内部是循环，结束了就是TTS结束了 generateAudio的callback传递null，避免递归导致栈溢出]
                checkIsTTSFinish(chatContextManager, this);

                // 回调上游代理
                additionalProxyCallback.onSingleComplete();
            }

            @Override
            public void onAllComplete() {
                log.info("[common TTS] onAllComplete 调用结束");
                // 完成
                chatContextManager.stopTTS();
                additionalProxyCallback.onAllComplete();
            }

            @Override
            public void onNext(String audioBase64Data) {
                // bytes -> base64Str
                Map<String, String> responseMap = new HashMap<>();
                responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.AUDIO_CHUNK.getType());
                responseMap.put(RealtimeResponseDataTypeEnum.DATA, audioBase64Data);
                String response = JSON.toJSONString(responseMap);
                webSocketMessageManager.submitMessage(
                        chatContextManager.agentId,
                        response
                );
                log.info("[common TTS] 发送TTS数据，tts data length: {}", audioBase64Data.length());
                additionalProxyCallback.onNext(audioBase64Data);
            }

            @Override
            public void haveNoSentence() {
                // 回调上游代理
                additionalProxyCallback.haveNoSentence();
            }

            @Override
            public void onError(Throwable throwable) {
                // 回调上游代理
                additionalProxyCallback.onError(throwable);
            }
        });
    }*/

//    /**
//     * 检查TTS是否结束 + 自我调用
//     * 内部是循环，结束了就是TTS结束了
//     * generateAudio的callback传递null，避免递归导致栈溢出
//     * @param chatContextManager    聊天上下文管理器
//     * @throws NoApiKeyException    无API密钥异常
//     * @throws UploadFileException  上传文件异常
//     */
//    private void checkIsTTSFinish(@NotNull RealtimeChatContextManager chatContextManager, @NotNull TTSStateCallback callback) throws NoApiKeyException, UploadFileException {
//        // 只有当LLM结束并且TTS的MQ不存在句子时候才跳出TTS自我调用: !(!LLMing && count <= 0)
//        if (!chatContextManager.isTTSFinallyFinish()){
//            boolean isLLMing = chatContextManager.llmProxyContext.isLLMing();
//            // 全部取出
//            String sentence = chatContextManager.llmProxyContext.getAllTTS();
//
//            // LLM还未结束但是句子数量为0，说明llm在缓存区存在一个很长的句子，此时就需要等待输出完毕
//            if (sentence.isEmpty() && isLLMing){
//                // TTS的MQ存在数据了 || LLM已经结束
//                while (chatContextManager.llmProxyContext.isLLMing()){
//                    try {
//                        log.info("[checkIsTTSFinish] LLM还未结束但是句子数量为0，说明llm在缓存区存在一个很长的句子，此时就需要等待输出完毕。等待LLM输出完毕...");
//                        Thread.sleep(10);
//                    } catch (InterruptedException e){
//                        log.info("[checkIsTTSFinish] 次线程任务被取消，休眠线程中断: {}", e.getMessage());
//                    }
//                }
//                String finishSentence = chatContextManager.llmProxyContext.getAllTTS();
//                log.info("[checkIsTTSFinish] finishSentence自我TTS调用: finishSentence: {}", finishSentence);
//                var ttsDisposable = ttsServiceService.generateAudioContinue(finishSentence, callback, chatContextManager::isTTSFinallyFinish);
//                // todo 任务添加到任务list
//            }
//            else if (!sentence.isEmpty()){
//                // 自我调用 (此时因为可能还是llm，循环仍然继续)
//                log.info("[checkIsTTSFinish] 自我调用TTS开始: sentence: {}", sentence);
//                // !(!LLMing && count <= 0)
//                var ttsDisposable = ttsServiceService.generateAudioContinue(sentence, callback, chatContextManager::isTTSFinallyFinish);
//                // todo 任务添加到任务list
//            }
//            // 是否自我调用
//            chatContextManager.llmProxyContext.setIsTTSCallSelf(true);
//        }
//        // 不是自我调用的结束点
//        boolean isTTSCallSelf = chatContextManager.llmProxyContext.isTTSCallSelf();
//        boolean isTTSFinallyFinish = chatContextManager.isTTSFinallyFinish();
//        if (!isTTSCallSelf && isTTSFinallyFinish){
//            log.info("[checkIsTTSFinish] 不是自我调用的结束点, isTTSCallSelf: {}, isTTSFinallyFinish: {}",
//                    false, true);
//            callback.onAllComplete();
//        }
//    }


//    @Override
//    public void startFunctionCallResultChat(@Nullable String imageBase64, @NotNull RealtimeChatContextManager chatContextManager, boolean isPassiveNotActive) throws NoApiKeyException, UploadFileException {
//        if (imageBase64 == null || imageBase64.isEmpty()) {
//            log.info("[websocket] 启动视觉聊天：无图片");
//            chatContextManager.addFunctionCallResult("[视觉任务结果]: error错误，用户并没有提供图片资源");
//            var disposable = functionCallResultLLMStreamCall(null, chatContextManager, isPassiveNotActive);
//            chatContextManager.addFunctionCallTask(disposable);
//        }
//        else {
//            log.info("[websocket] 启动视觉聊天：有图片, imageLength: {}", imageBase64.length());
//            String result = visionChatService.vlSingleFileBase64(imageBase64, chatContextManager.getUserRequestQuestion());
//            chatContextManager.addFunctionCallResult("[视觉任务结果]" + result);
//            var disposable = functionCallResultLLMStreamCall(result, chatContextManager, isPassiveNotActive);
//            chatContextManager.addFunctionCallTask(disposable);
//        }
//    }

//    @Override
//    public void handleImageCall(String imageBase64, @NotNull RealtimeChatContextManager chatContextManager) throws NoApiKeyException, UploadFileException {
//        if (imageBase64 == null || imageBase64.isEmpty()) {
//            log.info("[handleImageCall] 启动视觉聊天：无图片");
//            chatContextManager.visionContext.setVisionResult("无图片，视觉理解失败。");
//        }
//        else {
//            log.info("[handleImageCall] 启动视觉聊天：有图片, imageLength: {}", imageBase64.length());
//            String result = visionChatService.vlSingleFileBase64(imageBase64, chatContextManager.getUserRequestQuestion());
//            chatContextManager.visionContext.setVisionResult(result);
//        }
//    }


/*
    */
/**
     * functionCall工作流结果再次调用LLM回复
     * @param result                functionCall结果
     * @param chatContextManager    chatContextManager
     * @param isPassiveNotActive    是否被动调用, 有时候可能存在定时任务，主动调用functionCall，此时前端是没有接收到STRAT_TTS指令的，所以需要此值来控制是否添加启动值
     * @return                      用于取消的Disposable
     *//*

    private reactor.core.Disposable functionCallResultLLMStreamCall(@Nullable String result, @NotNull RealtimeChatContextManager chatContextManager, boolean isPassiveNotActive){
        var callback = new LLMStateCallback() {
            final StringBuffer textBuffer = new StringBuffer();
            @Override
            public void onSubscribe(Subscription subscription) {
                // 这里是functionCall的回复，所以需要进行agent headerId++
                int headerId = chatContextManager.addAgentMessageHeaderCount();
                log.info("[functionCallResultLLMStreamCall] 启动functionCall chat; headerId: {}", headerId);

                // 设置Agent message Time
                chatContextManager.currentAgentMessageTimestamp = System.currentTimeMillis();

                // function call result -> 是否是主动发起的function call
                if (!isPassiveNotActive){
                    log.info("[functionCallResultLLMStreamCall] 主动调用function call，发送起始标识符");
                    // 发送开始标识
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.START_TTS.getType());
                    responseMap.put(RealtimeResponseDataTypeEnum.DATA, RealtimeResponseDataTypeEnum.START_TTS.getType());
                    String startResponse = JSON.toJSONString(responseMap);
                    webSocketMessageManager.submitMessage(
                            chatContextManager.agentId,
                            startResponse
                    );
                }

                // 设置是首次TTS
                chatContextManager.setIsFirstTTS(true);

                // 设置tts是finalResultTTS
                chatContextManager.setIsFinalResultTTS(true);
            }

            @Override
            public void onFinish(SignalType signalType) {
                // 检查是否有剩余的
                String remainingText = optimizedSentenceDetector.extractAllCompleteSentences(textBuffer);
                if (StringUtils.hasText(remainingText)) {
                    log.info("[functionCallResultLLMStreamCall] LLM -> TTS 剩余: {}", remainingText);
                    // 添加到TTS MQ
                    chatContextManager.llmProxyContext.offerTTS(remainingText);
                }
                chatContextManager.stopLLM();
                log.info("[functionCallResultLLMStreamCall] LLM-Function结束");
            }

            @Override
            public void onNext(String fragment) {
                System.out.println("function call fragment = " + fragment);

                // 发送当前fragment消息
                chatContextManager.agentResponseStringBuffer.append(fragment);
                // 将新片段添加到缓冲区
                textBuffer.append(fragment);

                RealtimeChatTextResponse agentFragmentResponse = chatContextManager.getCurrentFragmentAgentResponse(fragment);

                // 发送消息给Client
                String agentFragmentResponseJson = JSON.toJSONString(agentFragmentResponse);
                Map<String, String> fragmentResponseMap = new HashMap<>();
                fragmentResponseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
                fragmentResponseMap.put(RealtimeResponseDataTypeEnum.DATA, agentFragmentResponseJson);
                String response = JSON.toJSONString(fragmentResponseMap);
                webSocketMessageManager.submitMessage(
                        chatContextManager.agentId,
                        response
                );

                // 是否是首次TTS
                if (chatContextManager.isFirstTTS().get()){
                    String complete1Sentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer);
                    if (StringUtils.hasText(complete1Sentence)){
                        chatContextManager.llmProxyContext.offerTTS(complete1Sentence);
                        var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                () -> {
                                    // 首次tts
                                    log.info("[functionCallResultLLMStreamCall] 首次TTS");
                                    var chatVisionTTSDisposable = proxyGenerateAudio(
                                            chatContextManager,
                                            true
                                    );
                                    chatContextManager.addFunctionCallTask(chatVisionTTSDisposable);
                                }
                        );
                        chatContextManager.addFunctionCallTask(ttsFuture);

                        // 设置不是首次TTS
                        chatContextManager.setIsFirstTTS(false);
                    }
                }
                else {
                    // 尝试从缓冲区提取2个完整句子并输出
                    String complete2Sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 2);
                    if (StringUtils.hasText(complete2Sentence)){
                        chatContextManager.llmProxyContext.offerTTS(complete2Sentence);
                    }
                }

                // 显示当前缓冲区剩余内容
                if (!textBuffer.isEmpty()) {
                    log.info("[functionCallResultLLMStreamCall] LLM 缓冲区剩余: {}", textBuffer);
                }
            }

            @Override
            public void haveNoSentence() {
                log.info("[functionCallResultLLMStreamCall] LLM-Function没有完整的句子");
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("[functionCallResultLLMStreamCall] LLM-Function错误", throwable);
            }
        };
        return llmServiceService.functionCallLLMStreamChat(
                result,
                chatContextManager.getUserRequestQuestion(),
                chatContextManager.getChatClient(),
                chatContextManager.getAgentId(),
                callback
        );
    }
*/

/*    *//**
     * 获取FunctionCall结果
     * @param chatContextManager    chatContextManager
     * @return                      AllFunctionCallFinished
     *//*
    @NotNull
    @Override
    public AllFunctionCallFinished getAllFunctionCallFinished(@NotNull RealtimeChatContextManager chatContextManager) {
        // 全部的Function Call完成
        return () -> {
            // 获取全部的FunctionCall结果
            String results = chatContextManager.llmProxyContext.getAllFunctionCallResult();
            // 被动调用
            var disposable = functionCallResultLLMStreamCall(results, chatContextManager, true);
            chatContextManager.addFunctionCallTask(disposable);
        };
    }*/
}
