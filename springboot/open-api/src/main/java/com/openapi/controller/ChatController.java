package com.openapi.controller;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.request.ChatRequest;
import com.openapi.domain.dto.resonse.ChatMessageResponse;
import com.openapi.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final DashScopeChatModel dashScopeChatModel;
    private final ChatMessageService chatMessageService;

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

    @GetMapping("/getLastChat")
    public BaseResponse<ChatMessageResponse> getLastChat(
            @RequestParam("agentId") String agentId
    ){
        // 参数校验
        if (!StringUtils.hasText(agentId)){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        val chatMessageDos = chatMessageService.getLast20Messages(agentId);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setChatMessageDos(chatMessageDos);

        return BaseResponse.getResponseEntitySuccess(response);
    }

    @GetMapping("/getTimeLimitChat")
    public BaseResponse<ChatMessageResponse> getTimeLimitChat(
            @RequestParam("agentId") String agentId,
            // yyyy-MM-dd HH:mm:ss
            @RequestParam("deadline") String deadline,
            // max 50
            @RequestParam("limit") Integer limit
    ){
        // 参数校验
        if (!StringUtils.hasText(agentId)){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        LocalDateTime time;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            time = LocalDateTime.parse(deadline, formatter);
        } catch (Exception e) {
            log.error("时间格式错误", e);
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (limit <= 0){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        else if (limit > 50){
            limit = 50;
        }

        val chatMessageDos = chatMessageService.getMessagesByAgentIdDeadlineLimit(agentId, time, limit);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setChatMessageDos(chatMessageDos);

        return BaseResponse.getResponseEntitySuccess(response);
    }

}
