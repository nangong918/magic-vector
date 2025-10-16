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
import com.openapi.converter.ChatMessageConverter;
import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.RoleTypeEnum;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.constant.error.UserExceptions;
import com.openapi.domain.constant.realtime.RealtimeDataTypeEnum;
import com.openapi.domain.dto.ws.RealtimeChatTextResponse;
import com.openapi.domain.exception.AppException;
import com.openapi.domain.interfaces.OnSSTResultCallback;
import com.openapi.service.AgentService;
import com.openapi.service.ChatMessageService;
import com.openapi.service.RealtimeChatService;
import com.openapi.service.UserService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Override
    public void startChat(@NotNull RealtimeChatContextManager chatContextManager, ChatClient chatClient) throws InterruptedException, NoApiKeyException {
        log.info("[startChat] 开始将音频流数据填充缓冲区");

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
            RealtimeChatTextResponse userAudioSttResponse = new RealtimeChatTextResponse();
            userAudioSttResponse.agentId = chatContextManager.agentId;
            userAudioSttResponse.userId = chatContextManager.userId;
            userAudioSttResponse.role = RoleTypeEnum.USER.getValue();
            userAudioSttResponse.content = result;
            userAudioSttResponse.messageId = chatContextManager.getCurrentUserMessageId();
            userAudioSttResponse.timestamp = chatContextManager.currentMessageTimestamp;
            userAudioSttResponse.chatTime = chatContextManager.currentMessageDateTime;
            String response = JSON.toJSONString(userAudioSttResponse);

            // 保存到数据库
            ChatMessageDo chatMessageDo = chatMessageConverter.realtimeChatTextResponseToChatMessageDo(userAudioSttResponse);
            String messageId = chatMessageService.insertOne(chatMessageDo);
            log.info("保存用户语音识别结果到数据库：{}", messageId);

            // 发送给Client
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.TEXT_MESSAGE.getType());
            responseMap.put(RealtimeDataTypeEnum.DATA, response);

            String startResponse = JSON.toJSONString(responseMap);

            chatContextManager.session.sendMessage(new TextMessage(startResponse));
            /// llm
            if (StringUtils.hasText(result)){
                llmStreamCall(result, chatContextManager, chatClient);
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

    private void llmStreamCall(String sentence, @NotNull RealtimeChatContextManager chatContextManager, ChatClient chatClient) {
        log.info("\n[LLM 开始] 输入内容: {}", sentence);

        Flux<String> responseFlux = chatClient.prompt(ModelConstant.SYSTEM_PROMPT)
                .user(sentence)
                .stream()
                .content();

        StringBuffer textBuffer = new StringBuffer();
        AtomicInteger fragmentCount = new AtomicInteger(0);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        // 发送开始标识
        try {
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.START.getType());
            responseMap.put(RealtimeDataTypeEnum.DATA, RealtimeDataTypeEnum.START.getType());
            String startResponse = JSON.toJSONString(responseMap);
            chatContextManager.session.sendMessage(new TextMessage(startResponse));
        } catch (IOException e) {
            log.error("[websocket error] 发送开始消息异常", e);
        }
        // 订阅流式响应并处理
        responseFlux.subscribe(

                // 处理每个流片段
                fragment -> {
                    chatContextManager.isLLMFinished.set(false);
                    int currentCount = fragmentCount.incrementAndGet();
                    long fragmentTime = System.currentTimeMillis() - startTime.get();

                    // 发送当前fragment消息
                    chatContextManager.currentResponse.append(fragment);
                    RealtimeChatTextResponse agentFragmentResponse = new RealtimeChatTextResponse();
                    agentFragmentResponse.setAgentId(chatContextManager.agentId);
                    agentFragmentResponse.setUserId(chatContextManager.userId);
                    agentFragmentResponse.setRole(RoleTypeEnum.AGENT.getValue());
                    agentFragmentResponse.setContent(chatContextManager.currentResponse.toString());
                    agentFragmentResponse.setMessageId(chatContextManager.getCurrentAgentMessageId());
                    agentFragmentResponse.setTimestamp(chatContextManager.currentMessageTimestamp);
                    agentFragmentResponse.setChatTime(chatContextManager.currentMessageDateTime);
                    // 存储消息到数据库
                    ChatMessageDo chatMessageDo = chatMessageConverter.realtimeChatTextResponseToChatMessageDo(agentFragmentResponse);
                    String messageId = chatMessageService.insertOne(chatMessageDo);
                    log.info("成功将LLM插入消息，消息Id: {}", messageId);

                    // 发送消息给Client
                    String agentFragmentResponseJson = JSON.toJSONString(agentFragmentResponse);
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.TEXT_MESSAGE.getType());
                    responseMap.put(RealtimeDataTypeEnum.DATA, agentFragmentResponseJson);
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

                    // 尝试从缓冲区提取完整句子并输出
                    String completeSentence;
                    while ((completeSentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer)) != null) {
                        /// tts
                        if (StringUtils.hasText(completeSentence)){
                            chatContextManager.sentenceQueue.add(completeSentence);
                            try {
                                // todo 对话延迟：第一个消息要立刻回答，后面的消息要延迟300ms合成
                                generateAudio(chatContextManager);
                            } catch (NoApiKeyException e) {
                                throw new RuntimeException(e);
                            } catch (UploadFileException e) {
                                throw new RuntimeException(e);
                            }
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
                    chatContextManager.isLLMFinished.set(true);
                },

                // 处理完成
                () -> {
                    long totalTime = System.currentTimeMillis() - startTime.get();
                    log.info("\n[LLM 结束] 总耗时: {}ms, 片段总数: {}, 总字符数: {}",
                            totalTime, fragmentCount.get(), textBuffer.length());

                    // 处理缓冲区中可能剩余的不完整内容
                    if (!textBuffer.isEmpty()) {
                        log.info("[最终剩余未完成内容]: {}", textBuffer);
                    }

                    chatContextManager.isLLMFinished.set(true);
                    sendEOF(chatContextManager);
                    log.info("[LLM 流式响应完全结束]");
                }
        );

    }

    private void generateAudio(@NotNull RealtimeChatContextManager chatContextManager) throws NoApiKeyException, UploadFileException {
        if (!chatContextManager.isTTSFinished.get()){
            return;
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

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(ModelConstant.TTS_Model)
                .apiKey(chatConfig.getApiKey())
                .text(sentence)
                .voice(ModelConstant.TTS_Voice)
                .languageType(ModelConstant.TTS_LanguageType)
                .build();

        Flowable<MultiModalConversationResult> result = multiModalConversation.streamCall(param);

        result.doOnSubscribe(
                        subscription -> log.info("TTS开始"))
                .doFinally(() -> {
                    chatContextManager.isTTSFinished.set(true);
                    sendEOF(chatContextManager);
                    if (!chatContextManager.sentenceQueue.isEmpty()){
                        log.info("[TTS]自我调用, 剩余数据: {}", chatContextManager.sentenceQueue.size());
                        // 自我调用
                        generateAudio(chatContextManager);
                    }
                })
                .blockingForEach(r -> {
                    String base64Data = r.getOutput().getAudio().getData();
                    if (base64Data != null && !base64Data.isEmpty()) {
                        byte[] audioBytes = Base64.getDecoder().decode(base64Data);

                        // bytes -> base64Str
                        String b64Audio = Base64.getEncoder().encodeToString(audioBytes);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.AUDIO_CHUNK.getType());
                        responseMap.put(RealtimeDataTypeEnum.DATA, b64Audio);
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
                responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.STOP.getType());
                responseMap.put(RealtimeDataTypeEnum.DATA, RealtimeDataTypeEnum.STOP.getType());
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
                .defaultSystem(description)
                .build();

        // 预先加载10条历史聊天记录
        List<ChatMessageDo> chatMessageDos = chatMessageService.getLast10Messages(chatContextManager.agentId);
        // 将历史消息添加到ChatMemory中
        if (!chatMessageDos.isEmpty()) {
            List<Message> historyMessages = chatMessageConverter.chatMessageDoListToMessageList(chatMessageDos);
            for (Message message : historyMessages) {
                chatMemory.add(chatContextManager.agentId, message);
            }
        }

        return chatClient;
    }
}
