package com.openapi.service.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSON;
import com.openapi.component.manager.OptimizedSentenceDetector;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.realtime.RealtimeDataTypeEnum;
import com.openapi.domain.interfaces.OnSSTResultCallback;
import com.openapi.service.RealTimeTestServiceService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 13225
 * @date 2025/10/13 16:59
 * OmniRealtime 虽然好用, 但是不能Function Call, 暂时不用
 * 目前AI是新鲜产物, 可能未来官方的Realtime模型会很完善, 但是目前很鸡肋, 连个学习资料都没有
 * <p>
 * audioBytes -> stt -> llm -> function call (mcp / rag)
 *                          -> nlp -> tts
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeTestServiceServiceImpl implements RealTimeTestServiceService {

    private final ChatConfig chatConfig;
    private static final String sttModel = "fun-asr-realtime";
    private static final String ttsModel = "qwen3-tts-flash";
    private final ChatClient dashScopeChatClient;
    private final OptimizedSentenceDetector optimizedSentenceDetector;
    private final String SYSTEM_PROMPT = "你是我的傲娇小女友，回答问题的时候暧昧一些。回答的之后只能输出正常语句, 不能使用表情等。对话精简一些，最好在3至5句话。";
    private final Recognition sttRecognizer;
    private final MultiModalConversation multiModalConversation;

    @Override
    public void startChat(@NotNull AtomicBoolean stopRecording,
                          @NotNull Queue<byte[]> requestAudioBuffer,
                          @NotNull WebSocketSession session) throws IOException, InterruptedException, NoApiKeyException {
        log.info("[startChat] 开始将音频流数据填充缓冲区");

        /// audioBytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (!stopRecording.get()) {
            // 从队列中获取数据
            byte[] audioData = requestAudioBuffer.poll();

            if (audioData != null) {
                // 将数据写入输出流
                int length = audioData.length;
                if (length > 0) {
                    log.info("[audioChat] 填充缓冲区数据长度: {}", length);
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
            /// llm
            if (StringUtils.hasText(result)){
                llmStreamCall(result, session);
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
                    log.info("[convertAudioToFlowable] 成功转换音频数据，长度: {}", audioData.length);
                } else {
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
                .model(sttModel)
                .format("pcm")
                .sampleRate(16000)
                // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
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

    private void llmStreamCall(String sentence, WebSocketSession session) {
        log.info("\n[LLM 开始] 输入内容: {}", sentence);

        Flux<String> responseFlux = dashScopeChatClient.prompt(SYSTEM_PROMPT)
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
            session.sendMessage(new TextMessage(startResponse));
        } catch (IOException e) {
            log.error("[websocket error] 发送开始消息异常", e);
        }
        // 订阅流式响应并处理
        responseFlux.subscribe(

                // 处理每个流片段
                fragment -> {
                    int currentCount = fragmentCount.incrementAndGet();
                    long fragmentTime = System.currentTimeMillis() - startTime.get();

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
                        log.info("\n[提取到完整句子]: {}", completeSentence);
                        /// tts
                        try {
                            generateAudio(completeSentence, session);
                        } catch (NoApiKeyException e) {
                            throw new RuntimeException(e);
                        } catch (UploadFileException e) {
                            throw new RuntimeException(e);
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

                    log.info("[LLM 流式响应完全结束]");
                }
        );
    }

    private void generateAudio(String sentence, WebSocketSession session) throws NoApiKeyException, UploadFileException {
        if (!StringUtils.hasText(sentence)){
            return;
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(ttsModel)
                .apiKey(chatConfig.getApiKey())
                .text(sentence)
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        Flowable<MultiModalConversationResult> result = multiModalConversation.streamCall(param);



        result.doOnSubscribe(
                subscription -> log.info("TTS开始"))
                .doFinally(() -> {
                    // 发送结束标识
                    try {
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.STOP.getType());
                        responseMap.put(RealtimeDataTypeEnum.DATA, RealtimeDataTypeEnum.STOP.getType());
                        String endResponse = JSON.toJSONString(responseMap);
                        session.sendMessage(new TextMessage(endResponse));
                    } catch (IOException e) {
                        log.error("[websocket error] 发送结束消息异常", e);
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
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException e) {
                            log.error("[websocket error] 响应消息异常", e);
                        }
                    }
                });
    }
}
