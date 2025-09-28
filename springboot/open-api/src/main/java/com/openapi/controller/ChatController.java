package com.openapi.controller;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.openapi.domain.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final DashScopeChatModel dashScopeChatModel;

    private final WebClient.Builder webClientBuilder;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();

        // 异步处理流
        return chatClient.prompt()
                .user(request.getQuestion())
                .stream()
                .content();
    }

}
