package com.openapi.controller;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.component.manager.OptimizedSentenceDetector;
import com.openapi.config.ChatConfig;
import com.openapi.domain.dto.request.ChatRequest;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.*;

@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final DashScopeChatModel dashScopeChatModel;

    /**
     * 流式聊天测试
     * @param request   请求体
     * @return          AI流式响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();

        // 异步处理流
        return chatClient.prompt()
                .user(request.getQuestion())
                .stream()
                .content();
    }

    /**
     * SSE流式聊天测试
     * @param request   请求体
     * @return          AI SSE流式响应
     */
    @PostMapping(value = "/stream-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sseStreamChat(@RequestBody ChatRequest request) {
        log.info("收到流式聊天请求: {}", request.getQuestion());

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();

        return chatClient.prompt()
                .user(request.getQuestion())
                .stream()
                .content()
                .map(data -> {
                    log.info("发送SSE数据: {}", data);
                    return ServerSentEvent.builder(data)
                            .id(UUID.randomUUID().toString())
                            .event("message")
                            .build();
                })
                .doOnSubscribe(subscription -> log.info("SSE连接建立"))
                .doOnComplete(() -> log.info("SSE流完成"))
                .doOnError(error -> log.error("SSE流错误", error));
    }

    @Autowired
    private ChatConfig config;

    @Autowired
    OptimizedSentenceDetector optimizedSentenceDetector;

    private static final String MODEL = "qwen3-tts-flash";

    @PostMapping(value = "/stream-tts-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> tts_sseStreamChat(@RequestBody ChatRequest request){
        log.info("收到流式TTS聊天请求: {}", request.getQuestion());

        // 创建一个Sinks.Many用于发射SSE事件
        Sinks.Many<ServerSentEvent<Map<String, String>>> sink = Sinks.many().unicast().onBackpressureBuffer();

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();
        MultiModalConversation conv = new MultiModalConversation();

        log.info("模型创建成功，开始调用.....");

        // 文本缓冲区
        StringBuffer textBuffer = new StringBuffer();
        String systemPrompt = "你是我的傲娇小女友，回答问题的时候暧昧一些。你只能输出自然语言，不要输出表情等特殊符号。";

        // 获取流式响应
        Flux<String> responseFlux = chatClient.prompt(systemPrompt)
                .user(request.getQuestion())
                .stream()
                .content();


        // 处理流式响应
        responseFlux.subscribe(
                // 处理每个流片段
                fragment -> {
                    try {
                        textBuffer.append(fragment);
                        log.info("接收到文本片段: {}", fragment);

                        // 发送原始片段（可选）
                        sendSseEvent(sink, "text", fragment);

                        // 提取完整句子并处理
                        String completeSentence;
                        while ((completeSentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer)) != null) {
                            log.info("[***提取到完整句子***]: {}", completeSentence);

                            // 生成并发送音频
                            generateAndSendAudio(completeSentence, conv, sink);
                        }
                    } catch (Exception e) {
                        log.error("处理文本片段错误", e);
                        sink.tryEmitError(e);
                    }
                },
                // 处理错误
                error -> {
                    log.error("流式处理错误", error);
                    sink.tryEmitError(error);
                },
                // 处理完成
                () -> {
                    log.info("流式响应结束");
                    sink.tryEmitComplete();
                }
        );

        return sink.asFlux();
    }

    /**
     * 发送SSE事件
     */
    private void sendSseEvent(Sinks.Many<ServerSentEvent<Map<String, String>>> sink, String type, String data) {
        Map<String, String> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("data", data);

        ServerSentEvent<Map<String, String>> event = ServerSentEvent.<Map<String, String>>builder()
                .id(UUID.randomUUID().toString())
                .event(type)
                .data(payload)
                .build();

        sink.tryEmitNext(event);
    }

    /**
     * 生成并发送音频
     */
    private void generateAndSendAudio(String text, MultiModalConversation ttsClient, Sinks.Many<ServerSentEvent<Map<String, String>>> sink)
            throws ApiException, NoApiKeyException, UploadFileException {
        // 构建TTS请求参数
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .text(text)
                .apiKey(config.getApiKey())
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        // 调用TTS服务
        Flowable<MultiModalConversationResult> result = ttsClient.streamCall(param);

        result.blockingForEach(r -> {
            try {
                String base64Data = r.getOutput().getAudio().getData();
                sendSseEvent(sink, "audio", base64Data);
            } catch (Exception e) {
                log.error("处理音频片段错误", e);
            }
        });
    }


}
