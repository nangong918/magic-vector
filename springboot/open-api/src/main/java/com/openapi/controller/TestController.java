package com.openapi.controller;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.openapi.config.ChatConfig;
import com.openapi.domain.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
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

    private static final String MODEL = "qwen3-tts-flash";

    @PostMapping(value = "/stream-tts-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> tts_sseStreamChat(@RequestBody ChatRequest request){
        log.info("收到流式TTS聊天请求: {}", request.getQuestion());

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();
        MultiModalConversation conv = new MultiModalConversation();


        Flux.create(emitter -> {
            try {
                // 1. 先获取完整的文本流
                List<String> textBuffer = new ArrayList<>();
                StringBuilder currentSentence = new StringBuilder();

                chatClient.prompt()
                        .user(request.getQuestion())
                        .stream()
                        .content()
                        .map(data -> {
                            log.info("发送SSE-Text数据: {}", data);
                            // 拼接音频数据
                            currentSentence.append(data);
                            // 发送文本数据
                            Map<String, String> textData = new HashMap<>();
                            textData.put("type", "text");
                            textData.put("data", data);
                            return ServerSentEvent.builder(textData)
                                    .id(UUID.randomUUID().toString())
                                    .event("message")
                                    .build();
                        })
                        .doOnSubscribe(subscription -> log.info("SSE-Text连接建立"))
                        .doOnComplete(() -> log.info("SSE-Text流完成"))
                        .doOnError(error -> log.error("SSE-Text流错误", error));
            } catch (Exception e){

            }
        });
        return null;
    }


}
