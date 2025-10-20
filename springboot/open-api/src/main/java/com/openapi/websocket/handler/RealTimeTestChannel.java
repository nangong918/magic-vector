package com.openapi.websocket.handler;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openapi.domain.constant.realtime.RealtimeDataTypeEnum;
import com.openapi.service.RealTimeTestServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/10/13 18:23
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RealTimeTestChannel extends TextWebSocketHandler {

    private final RealTimeTestServiceService realTimeTestServiceService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private volatile Future<?> audioChatFuture;
    private volatile Future<?> textChatFuture;
    private ChatClient chatClient;
    private final DashScopeChatModel dashScopeChatModel;


    private final Queue<byte[]> requestAudioBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean stopRecording = new AtomicBoolean(true);

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        log.info("[websocket] 创建连接：id={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        log.info("[websocket] 连接断开：id={}，reason={}", session.getId(), status);
        stopRecording.set(true);
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.info("[websocket] 连接异常：id={}，throwable={}", session.getId(), exception.getMessage());
        session.close();
        stopRecording.set(true);
        super.handleTransportError(session, exception);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws Exception {

        Map<String, String> messageMap = JSON.parseObject(message.getPayload(), new TypeReference<>() {});
        String type = messageMap.get(RealtimeDataTypeEnum.TYPE);
        if (!StringUtils.hasText(type)) {
            log.warn("[websocket warn] 收到消息，类型为空");
            return;
        }

        RealtimeDataTypeEnum realtimeDataTypeEnum = RealtimeDataTypeEnum.getByType(type);
        switch (realtimeDataTypeEnum) {
            case CONNECT -> {
                chatClient = realTimeTestServiceService.initChatClient(dashScopeChatModel);
                log.info("[websocket] CONNECT::chatClient初始化");
            }
            case START -> {
                log.info("[websocket] 开始录音");
                stopRecording.set(false);
                audioChatFuture = taskExecutor.submit(() -> {
                    try {
                        // 启动聊天
                        realTimeTestServiceService.startChat(
                                stopRecording,
                                requestAudioBuffer,
                                session
                        );
                    } catch (Exception e) {
                        stopRecording.set(true);
                        log.error("[audioChat] 聊天处理异常", e);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.STOP.getType());
                        responseMap.put(RealtimeDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException ex) {
                            log.error("[websocket error] 响应消息异常", ex);
                        }
                    }
                });
            }
            case STOP -> {
                log.info("[websocket] 停止录音");
                stopRecording.set(true);
            }
            case AUDIO_CHUNK -> {
                log.info("[websocket] 收到音频块");
                handleAudioChunk(messageMap.get(RealtimeDataTypeEnum.DATA));
            }
            case TEXT_MESSAGE -> {
                String userQuestion = messageMap.get(RealtimeDataTypeEnum.DATA);
                textChatFuture = taskExecutor.submit(() -> {
                    try {
                        // 启动聊天
                        realTimeTestServiceService.startTextChat(
                                userQuestion,
                                session,
                                chatClient
                        );
                    } catch (Exception e) {
                        stopRecording.set(true);
                        log.error("[TextChat] 聊天处理异常", e);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeDataTypeEnum.TYPE, RealtimeDataTypeEnum.STOP.getType());
                        responseMap.put(RealtimeDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException ex) {
                            log.error("[websocket error] 响应消息异常", ex);
                        }
                    }
                });
            }
            default -> log.warn("[websocket warn] 忽略未知类型消息：{}", type);
        }
    }

    @Override
    protected void handleBinaryMessage(@NotNull WebSocketSession session, BinaryMessage message) {
        log.info("[websocket] 收到二进制消息，长度：{}", message.getPayloadLength());
    }

    private void handleAudioChunk(String base64Audio) {
        if (!StringUtils.hasText(base64Audio)){
            return;
        }
        // 解码 Base64 字符串为字节数组
        byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
//        log.info("[websocket] 音频块大小：{}", audioBytes.length);

        // 将字节数组放入队列
        requestAudioBuffer.offer(audioBytes);
    }

}
