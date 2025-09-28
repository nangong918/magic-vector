package com.openapi.controller;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.openapi.domain.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;


@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final DashScopeChatModel dashScopeChatModel;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();

        // 异步处理流
        return chatClient.prompt()
                .user(request.getQuestion())
                .stream()
                .content();
    }

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

}
