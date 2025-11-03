package com.openapi.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSON;
import com.openapi.component.manager.OptimizedSentenceDetector;
import com.openapi.component.manager.RealtimeChatContextManager;
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
import com.openapi.domain.interfaces.OnSSTResultCallback;
import com.openapi.service.AgentService;
import com.openapi.service.ChatMessageService;
import com.openapi.service.PromptService;
import com.openapi.service.RealtimeChatService;
import com.openapi.service.UserService;
import com.openapi.service.VisionChatService;
import com.openapi.service.VisionToolService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.socket.TextMessage;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    private final MultiModalConversation multiModalConversation;
    private final ChatMessageService chatMessageService;
    private final ChatMessageConverter chatMessageConverter;
    private final AgentConfig agentConfig;
    private final UserService userService;
    private final AgentService agentService;
    private final ThreadPoolConfig threadPoolConfig;
    private final PromptService promptService;
    private final VisionToolService visionToolService;
    private final VisionChatService visionChatService;

    @Override
    public void startChat(@NotNull RealtimeChatContextManager chatContextManager) throws InterruptedException, NoApiKeyException {
        log.info("[startChat] 开始将音频流数据填充缓冲区");

        var chatClient = chatContextManager.chatClient;
        if (chatClient == null){
            throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
        }

        /// audioBytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (!chatContextManager.stopRecording.get()) {
            // 从队列中获取数据
            byte[] audioData = chatContextManager.requestAudioBuffer.poll();

            if (audioData != null) {
                // 将数据写入输出流
                int length = audioData.length;
                if (length > 0) {
                    out.write(audioData, 0, length);
                }
            }
            else {
                // 10 毫秒休眠
                Thread.sleep(10);
            }
        }

        // 发送录音数据
        byte[] audioData = out.toByteArray();

        /// stt
        Flowable<ByteBuffer> audioFlowable = convertAudioToFlowable(audioData);
        sttStreamCall(audioFlowable, result -> {
            // 返回结果给前端
            RealtimeChatTextResponse userAudioSttResponse = chatContextManager.getSSTResultResponse(result);
            String response = JSON.toJSONString(userAudioSttResponse);

            // 保存到数据库
            ChatMessageDo chatMessageDo = null;
            try {
                chatMessageDo = chatMessageConverter.realtimeChatTextResponseToChatMessageDo(userAudioSttResponse, userAudioSttResponse.chatTime);
                String messageId = chatMessageService.insertOne(chatMessageDo);
                log.info("保存用户语音识别结果到数据库：{}", messageId);
            } catch (Exception e) {
                log.error("保存用户语音识别结果到数据库失败：{}", e.getMessage());
            }

            // 发送给Client
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
            responseMap.put(RealtimeResponseDataTypeEnum.DATA, response);

            String startResponse = JSON.toJSONString(responseMap);

            chatContextManager.session.sendMessage(new TextMessage(startResponse));
            /// llm
            if (StringUtils.hasText(result)) {
                llmStreamCall(result, chatContextManager);
            }
        });
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

    private void sttStreamCall(Flowable<ByteBuffer> audioSource, OnSSTResultCallback callback) throws NoApiKeyException {
        // 创建RecognitionParam，audioFrames参数中传入上面创建的Flowable<ByteBuffer>
        RecognitionParam sttParam = RecognitionParam.builder()
                .model(ModelConstant.STT_Model)
                .format(ModelConstant.STT_Format)
                .sampleRate(ModelConstant.SST_SampleRate)
                .apiKey(chatConfig.getApiKey())
                .build();

        sttRecognizer.streamCall(sttParam, audioSource)
                .blockingForEach(
                        result -> {
                            // 打印最终结果
                            if (result.isSentenceEnd()) {
                                String sentence = result.getSentence().getText();
                                callback.onResult(sentence);
                                log.info("[sttStreamCall] 识别结果: {}", sentence);
                            }
                        }
                );
    }

    private void llmStreamCall(String sentence, @NotNull RealtimeChatContextManager chatContextManager) /*throws WebClientRequestException*/ {
        log.info("\n[LLM 开始] 输入内容: {}", sentence);

        var chatClient = chatContextManager.chatClient;
        if (chatClient == null){
            throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
        }

        String param = chatContextManager.getCurrentContextParam();
        String systemPrompt = chatConfig.getTextFunctionCallPrompt(param);
        Prompt prompt = promptService.getChatPromptWhitSystemPrompt(
                sentence,
                systemPrompt
        );

        if (prompt == null){
            log.error("[LLM 提示词] 获取失败");
            sendEOF(chatContextManager);
            return;
        }

        Flux<String> responseFlux = chatClient.prompt(prompt)
//                .user(sentence)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatContextManager.agentId))
                // 添加工具Function Call; MCP
                .tools(visionToolService)
                .stream()
                .content()
                // 3500ms未响应则判定超时，进行重连尝试
                .timeout(Duration.ofMillis(ModelConstant.LLM_CONNECT_TIMEOUT_MILLIS));

        StringBuffer textBuffer = new StringBuffer();
        AtomicInteger fragmentCount = new AtomicInteger(0);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        // 设置Agent message Time
        chatContextManager.currentAgentMessageTimestamp = System.currentTimeMillis();

        // 发送开始标识
        try {
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.START_TTS.getType());
            responseMap.put(RealtimeResponseDataTypeEnum.DATA, RealtimeResponseDataTypeEnum.START_TTS.getType());
            String startResponse = JSON.toJSONString(responseMap);
            chatContextManager.session.sendMessage(new TextMessage(startResponse));
        } catch (IOException e) {
            log.error("[websocket error] 发送开始消息异常", e);
        }
        // 订阅流式响应并处理
        responseFlux.subscribe(

                // 处理每个流片段
                fragment -> {
                    log.info("[LLM] 片段: {}", fragment);
                    chatContextManager.isLLMFinished.set(false);
                    int currentCount = fragmentCount.incrementAndGet();
                    long fragmentTime = System.currentTimeMillis() - startTime.get();

                    // 发送当前fragment消息
                    chatContextManager.currentResponseStringBuffer.append(fragment);
//                    RealtimeChatTextResponse agentFragmentResponse = chatContextManager.getCurrentResponse();
                    RealtimeChatTextResponse agentFragmentResponse = chatContextManager.getCurrentFragmentResponse(fragment);

                    // 发送消息给Client
                    String agentFragmentResponseJson = JSON.toJSONString(agentFragmentResponse);
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
                    responseMap.put(RealtimeResponseDataTypeEnum.DATA, agentFragmentResponseJson);
                    String response = JSON.toJSONString(responseMap);
                    try {
                        chatContextManager.session.sendMessage(new TextMessage(response));
                    } catch (IOException e) {
                        log.error("[websocket error] 响应消息异常", e);
                    }

                    // 观察片段信息
                    log.info("\n[LLM 片段 #{}, 耗时: {}ms]", currentCount, fragmentTime);
                    log.info("[片段内容]: {}", fragment);
                    log.info("[片段长度]: {} 字符", fragment.length());

                    // 将新片段添加到缓冲区
                    textBuffer.append(fragment);
                    log.info("[缓冲区累计]: {} 字符", textBuffer.length());

                    if (chatContextManager.isFirstTTS.compareAndSet(true, false)){
                        // 首次tts
                        log.info("[llm call] 首次tts");

                        String complete1Sentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer);
                        if (StringUtils.hasText(complete1Sentence)){
                            chatContextManager.sentenceQueue.add(complete1Sentence);
                            var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                    () -> generateAudio(chatContextManager)
                            );
                            chatContextManager.setTtsFuture(ttsFuture);
                        }
                    }
                    else {
                        log.info("[llm call] 非首次tts");

                        // 尝试从缓冲区提取2个完整句子并输出
                        String complete2Sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 2);
                        if (StringUtils.hasText(complete2Sentence)){
                            chatContextManager.sentenceQueue.add(complete2Sentence);
                            var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                    () -> generateAudio(chatContextManager)
                            );
                            chatContextManager.setTtsFuture(ttsFuture);
                        }
                    }

                    // 显示当前缓冲区剩余内容
                    if (!textBuffer.isEmpty()) {
                        log.info("[缓冲区剩余]: {}", textBuffer);
                    }

                    // 更新最后活跃时间
                    startTime.set(System.currentTimeMillis());
                },

                // 处理错误
                error -> {
                    long totalTime = System.currentTimeMillis() - startTime.get();
                    log.error("\n[LLM 错误] 总耗时: {}ms, 片段总数: {}", totalTime, fragmentCount.get(), error);

                    // (待优化A1)长时间未连接之后再次连接会发生Connect Reset目前采用的是递归的方式重试，可能造成堆栈溢出。考虑改为循环调用的方式
                    // 尝试再次自我调用
                    if (error instanceof WebClientRequestException || error instanceof TimeoutException){
                        log.error("[LLM 错误] 尝试再次自我调用；错误信息, 错误类型: ", error);
//                        // 再次自调用
                        if (chatContextManager.llmConnectResetRetryCount.get() < ModelConstant.LLM_CONNECT_RESET_MAX_RETRY_COUNT){
                            int attempt = chatContextManager.llmConnectResetRetryCount.incrementAndGet();
                            log.warn("检测到连接重置，进行第{}次重试", attempt);

                            llmStreamCall(sentence, chatContextManager);
                        }
                        else {
                            log.error("[LLM 错误] 连接重置次数过多，已尝试{}次，放弃重试", chatContextManager.llmConnectResetRetryCount.get());
                            chatContextManager.isLLMFinished.set(true);
                        }
                    }
                    else {
                        log.error("[LLM 错误] 错误信息, 并非WebClientRequestException问题", error);
                        chatContextManager.isLLMFinished.set(true);
                    }

                    // 重置重连次数
                    chatContextManager.llmConnectResetRetryCount.set(0);
                },

                // 处理完成
                () -> {
                    long totalTime = System.currentTimeMillis() - startTime.get();
                    log.info("\n[LLM 结束] 总耗时: {}ms, 片段总数: {}, 总字符数: {}",
                            totalTime, fragmentCount.get(), textBuffer.length());

                    // 检查是否有剩余的
                    String remainingText = optimizedSentenceDetector.extractAllCompleteSentences(textBuffer);
                    if (StringUtils.hasText(remainingText)) {
                        log.info("[LLM -> TTS 剩余]: {}", remainingText);
                        chatContextManager.sentenceQueue.add(remainingText);
                        var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                () -> generateAudio(chatContextManager)
                        );
                        chatContextManager.setTtsFuture(ttsFuture);
                    }

                    // 存储消息到数据库
                    val realtimeChatTextResponse = chatContextManager.getCurrentResponse();
                    ChatMessageDo chatMessageDo = null;
                    try {
                        chatMessageDo = chatMessageConverter.realtimeChatTextResponseToChatMessageDo(
                                realtimeChatTextResponse,
                                realtimeChatTextResponse.getChatTime()
                        );
                        String messageId = chatMessageService.insertOne(chatMessageDo);
                        log.info("成功将LLM插入消息，消息Id: {}", messageId);
                    } catch (Exception e) {
                        log.error("[LLM 错误] 存储消息异常", e);
                    }

                    // 处理缓冲区中可能剩余的不完整内容
                    if (!textBuffer.isEmpty()) {
                        log.info("[最终剩余未完成内容]: {}", textBuffer);
                    }

                    chatContextManager.isLLMFinished.set(true);
                    // 发送结束符EOF（内部包含检查tts是否完成，不用担心）
                    sendEOF(chatContextManager);
                    log.info("[LLM 流式响应完全结束]");
                }
        );

    }

    private void generateAudio(@NotNull RealtimeChatContextManager chatContextManager) {
        if (!chatContextManager.isTTSFinished.get()){
            log.info("[TTS] 正在生成音频，请稍等...");
            return;
        }

        long ttsGapTime = System.currentTimeMillis() - chatContextManager.lastTTSTimestamp;
        if (ttsGapTime < ModelConstant.SENTENCE_INTERVAL){
            try {
                log.info("[TTS] 模拟人语音停顿 暂停 {}ms", ModelConstant.SENTENCE_INTERVAL - ttsGapTime);
                Thread.sleep(ModelConstant.SENTENCE_INTERVAL - ttsGapTime);
            } catch (InterruptedException e) {
                log.error("[TTS] 线程中断", e);
            }
        }
        else {
            log.info("[TTS] 虚需模拟人语音停顿 已经耗时 {}ms", ttsGapTime);
        }

        // 全部取出
        List<String> sentences = new ArrayList<>();
        while (!chatContextManager.sentenceQueue.isEmpty()) {
            sentences.add(chatContextManager.sentenceQueue.poll());
        }
        if (sentences.isEmpty()){
            log.info("[TTS] 暂无数据");
            chatContextManager.isTTSFinished.set(true);
            return;
        }

        // 拼接
        StringBuilder sb = new StringBuilder();
        for (String sentence : sentences) {
            sb.append(sentence);
        }
        String sentence = sb.toString();
        if (!StringUtils.hasText(sentence)){
            log.info("[TTS] 暂无数据");
            chatContextManager.isTTSFinished.set(true);
            return;
        }
        else {
            chatContextManager.isTTSFinished.set(false);
            log.info("[TTS] 输入内容: {}", sentence);
        }

        /// @see RealtimeResponseDataTypeEnum.WHOLE_CHAT_RESPONSE; 整句回复在此处，暂时不开发

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(ModelConstant.TTS_Model)
                .apiKey(chatConfig.getApiKey())
                .text(sentence)
                .voice(ModelConstant.TTS_Voice)
                .languageType(ModelConstant.TTS_LanguageType)
                .build();

        Flowable<MultiModalConversationResult> result = null;
        try {
            result = multiModalConversation.streamCall(param);
        } catch (NoApiKeyException e) {
            log.error("[tts] NoApiKeyException", e);
            throw new RuntimeException(e);
        } catch (UploadFileException e) {
            log.error("[tts] UploadFileException", e);
            throw new RuntimeException(e);
        }

        result.doOnSubscribe(
                        subscription -> log.info("TTS开始"))
                .doFinally(() -> {
                    chatContextManager.isTTSFinished.set(true);
                    // 记录结束时间
                    chatContextManager.lastTTSTimestamp = System.currentTimeMillis();
                    if (!chatContextManager.sentenceQueue.isEmpty()){
                        log.info("[TTS]自我调用, 剩余数据: {}", chatContextManager.sentenceQueue.size());
                        // 自我调用
                        var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                () -> generateAudio(chatContextManager)
                        );
                        chatContextManager.setTtsFuture(ttsFuture);
                    }
                    else {
                        log.info("[TTS]结束 发送EOF");
                        sendEOF(chatContextManager);
                    }
                })
                .blockingForEach(r -> {
                    String base64Data = r.getOutput().getAudio().getData();
                    if (base64Data != null && !base64Data.isEmpty()) {
                        byte[] audioBytes = Base64.getDecoder().decode(base64Data);

                        // bytes -> base64Str
                        String b64Audio = Base64.getEncoder().encodeToString(audioBytes);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.AUDIO_CHUNK.getType());
                        responseMap.put(RealtimeResponseDataTypeEnum.DATA, b64Audio);
                        String response = JSON.toJSONString(responseMap);
                        try {
                            chatContextManager.session.sendMessage(new TextMessage(response));
                        } catch (IOException e) {
                            log.error("[websocket error] 响应消息异常", e);
                        }
                    }
                });
    }

    private void sendEOF(@NotNull RealtimeChatContextManager chatContextManager){
        if (chatContextManager.isTTSFinished.get() && chatContextManager.isLLMFinished.get()) {
            // 发送结束标识
            try {
                // 当前的合成音频播放完成之后不代表全合成音频都播放完成了, 因此此处不能发送EOF
                Map<String, String> responseMap = new HashMap<>();
                responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                responseMap.put(RealtimeResponseDataTypeEnum.DATA, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                String endResponse = JSON.toJSONString(responseMap);
                chatContextManager.session.sendMessage(new TextMessage(endResponse));
                log.info("[LLM 流式响应结束]");
            } catch (IOException e) {
                log.error("[websocket error] 发送结束消息异常", e);
            }
        }
    }

    /**
     * 初始化ChatClient
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


    @Override
    public void startTextChat(@NotNull String userQuestion, @NotNull RealtimeChatContextManager chatContextManager) throws AppException, IOException {
        log.info("[websocket] 开始文本聊天：userQuestion={}", userQuestion);

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

        // 发送给Client
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
        responseMap.put(RealtimeResponseDataTypeEnum.DATA, response);

        String startResponse = JSON.toJSONString(responseMap);
        chatContextManager.session.sendMessage(new TextMessage(startResponse));

        llmStreamCall(userQuestion, chatContextManager);
    }

    @Override
    public void startVisionChat(@Nullable String imageBase64, @NotNull RealtimeChatContextManager chatContextManager) throws NoApiKeyException, UploadFileException {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            log.info("[websocket] 启动视觉聊天：无图片");
            visionResultLLMStreamCall(null, chatContextManager);
        }
        else {
            log.info("[websocket] 启动视觉聊天：有图片, imageLength: {}", imageBase64.length());
            String result = visionChatService.callWithFileBase64(imageBase64, chatContextManager.getUserQuestion());
            visionResultLLMStreamCall(result, chatContextManager);
        }
    }

    // todo 优化代码，将visionLLM和原先的llm相同逻辑合并管理
    private void visionResultLLMStreamCall(@Nullable String result, @NotNull RealtimeChatContextManager chatContextManager){
        log.info("\n[vision LLM 开始] vision识别内容: {}", result);

        var chatClient = chatContextManager.chatClient;
        if (chatClient == null){
            throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
        }

        String systemPrompt = chatConfig.getVisionLLMPrompt(result);

        Prompt prompt = promptService.getChatPromptWhitSystemPrompt(
                chatContextManager.getUserQuestion(),
                systemPrompt
        );
        log.info("[vision LLM 提示词] {}", prompt);

        if (prompt == null){
            log.error("[vision LLM 提示词] 获取失败");
            sendEOF(chatContextManager);
            return;
        }

        Flux<String> responseFlux = chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatContextManager.agentId))
                .stream()
                .content()
                // 3500ms未响应则判定超时，进行重连尝试
                .timeout(Duration.ofMillis(ModelConstant.LLM_CONNECT_TIMEOUT_MILLIS));

        StringBuffer textBuffer = new StringBuffer();
        AtomicInteger fragmentCount = new AtomicInteger(0);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        // 设置Agent message Time
        chatContextManager.currentAgentMessageTimestamp = System.currentTimeMillis();

        // 发送开始标识
        try {
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.START_TTS.getType());
            responseMap.put(RealtimeResponseDataTypeEnum.DATA, RealtimeResponseDataTypeEnum.START_TTS.getType());
            String startResponse = JSON.toJSONString(responseMap);
            chatContextManager.session.sendMessage(new TextMessage(startResponse));
        } catch (IOException e) {
            log.error("[websocket error] 发送开始消息异常", e);
        }
        // 订阅流式响应并处理
        responseFlux.subscribe(

                // 处理每个流片段
                fragment -> {
                    log.info("[LLM] 片段: {}", fragment);
                    chatContextManager.isLLMFinished.set(false);
                    int currentCount = fragmentCount.incrementAndGet();
                    long fragmentTime = System.currentTimeMillis() - startTime.get();

                    // 发送当前fragment消息
                    chatContextManager.currentResponseStringBuffer.append(fragment);
//                    RealtimeChatTextResponse agentFragmentResponse = chatContextManager.getCurrentResponse();
                    RealtimeChatTextResponse agentFragmentResponse = chatContextManager.getCurrentFragmentResponse(fragment);

                    // 发送消息给Client
                    String agentFragmentResponseJson = JSON.toJSONString(agentFragmentResponse);
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
                    responseMap.put(RealtimeResponseDataTypeEnum.DATA, agentFragmentResponseJson);
                    String response = JSON.toJSONString(responseMap);
                    try {
                        chatContextManager.session.sendMessage(new TextMessage(response));
                    } catch (IOException e) {
                        log.error("[websocket error] 响应消息异常", e);
                    }

                    // 观察片段信息
                    log.info("\n[LLM 片段 #{}, 耗时: {}ms]", currentCount, fragmentTime);
                    log.info("[片段内容]: {}", fragment);
                    log.info("[片段长度]: {} 字符", fragment.length());

                    // 将新片段添加到缓冲区
                    textBuffer.append(fragment);
                    log.info("[缓冲区累计]: {} 字符", textBuffer.length());

                    if (chatContextManager.isFirstTTS.compareAndSet(true, false)){
                        // 首次tts
                        log.info("[llm call] 首次tts");

                        String complete1Sentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer);
                        if (StringUtils.hasText(complete1Sentence)){
                            chatContextManager.sentenceQueue.add(complete1Sentence);
                            var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                    () -> generateAudio(chatContextManager)
                            );
                            chatContextManager.setTtsFuture(ttsFuture);
                        }
                    }
                    else {
                        log.info("[llm call] 非首次tts");

                        // 尝试从缓冲区提取2个完整句子并输出
                        String complete2Sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 2);
                        if (StringUtils.hasText(complete2Sentence)){
                            chatContextManager.sentenceQueue.add(complete2Sentence);
                            var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                    () -> generateAudio(chatContextManager)
                            );
                            chatContextManager.setTtsFuture(ttsFuture);
                        }
                    }

                    // 显示当前缓冲区剩余内容
                    if (!textBuffer.isEmpty()) {
                        log.info("[缓冲区剩余]: {}", textBuffer);
                    }

                    // 更新最后活跃时间
                    startTime.set(System.currentTimeMillis());
                },

                // 处理错误
                error -> {
                    long totalTime = System.currentTimeMillis() - startTime.get();
                    log.error("\n[LLM 错误] 总耗时: {}ms, 片段总数: {}", totalTime, fragmentCount.get(), error);

                    // (待优化A1)长时间未连接之后再次连接会发生Connect Reset目前采用的是递归的方式重试，可能造成堆栈溢出。考虑改为循环调用的方式
                    // 尝试再次自我调用
                    if (error instanceof WebClientRequestException || error instanceof TimeoutException){
                        log.error("[LLM 错误] 尝试再次自我调用；错误信息, 错误类型: ", error);
//                        // 再次自调用
                        if (chatContextManager.llmConnectResetRetryCount.get() < ModelConstant.LLM_CONNECT_RESET_MAX_RETRY_COUNT){
                            int attempt = chatContextManager.llmConnectResetRetryCount.incrementAndGet();
                            log.warn("检测到连接重置，进行第{}次重试", attempt);

                            visionResultLLMStreamCall(result, chatContextManager);
                        }
                        else {
                            log.error("[LLM 错误] 连接重置次数过多，已尝试{}次，放弃重试", chatContextManager.llmConnectResetRetryCount.get());
                            chatContextManager.isLLMFinished.set(true);
                        }
                    }
                    else {
                        log.error("[LLM 错误] 错误信息, 并非WebClientRequestException问题", error);
                        chatContextManager.isLLMFinished.set(true);
                    }

                    // 重置重连次数
                    chatContextManager.llmConnectResetRetryCount.set(0);
                },

                // 处理完成
                () -> {
                    long totalTime = System.currentTimeMillis() - startTime.get();
                    log.info("\n[LLM 结束] 总耗时: {}ms, 片段总数: {}, 总字符数: {}",
                            totalTime, fragmentCount.get(), textBuffer.length());

                    // 检查是否有剩余的
                    String remainingText = optimizedSentenceDetector.extractAllCompleteSentences(textBuffer);
                    if (StringUtils.hasText(remainingText)) {
                        log.info("[LLM -> TTS 剩余]: {}", remainingText);
                        chatContextManager.sentenceQueue.add(remainingText);
                        var ttsFuture = threadPoolConfig.taskExecutor().submit(
                                () -> generateAudio(chatContextManager)
                        );
                        chatContextManager.setTtsFuture(ttsFuture);
                    }

                    // 存储消息到数据库
                    val realtimeChatTextResponse = chatContextManager.getCurrentResponse();
                    ChatMessageDo chatMessageDo = null;
                    try {
                        chatMessageDo = chatMessageConverter.realtimeChatTextResponseToChatMessageDo(
                                realtimeChatTextResponse,
                                realtimeChatTextResponse.getChatTime()
                        );
                        String messageId = chatMessageService.insertOne(chatMessageDo);
                        log.info("成功将LLM插入消息，消息Id: {}", messageId);
                    } catch (Exception e) {
                        log.error("[LLM 错误] 存储消息异常", e);
                    }

                    // 处理缓冲区中可能剩余的不完整内容
                    if (!textBuffer.isEmpty()) {
                        log.info("[最终剩余未完成内容]: {}", textBuffer);
                    }

                    chatContextManager.isLLMFinished.set(true);
                    // 发送结束符EOF（内部包含检查tts是否完成，不用担心）
                    sendEOF(chatContextManager);
                    log.info("[LLM 流式响应完全结束]");
                }
        );
    }
}
