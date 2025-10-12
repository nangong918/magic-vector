package com.openapi.websocket.handler;

import com.alibaba.dashscope.audio.omni.OmniRealtimeConversation;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openapi.domain.constant.realtime.RealtimeDataTypeEnum;
import com.openapi.service.OmniRealTimeNoVADTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
 * @date 2025/10/7 16:45
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OmniRealTimeNoVADTestChannel extends TextWebSocketHandler {

    private final OmniRealTimeNoVADTestService omniRealTimeNoVADTestService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private volatile Future<?> chatFuture;

    private final Queue<byte[]> requestAudioBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean stopRecording = new AtomicBoolean(true);
    // AI会话；非单例，不能注入，会话是每个连接单独一个
    private OmniRealtimeConversation conversation;

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        log.info("[websocket] 创建连接：id={}", session.getId());

        try {
            conversation = omniRealTimeNoVADTestService.getOmniRealtimeConversation(
                    session
            );
            omniRealTimeNoVADTestService.setOmniRealtimeConversationConfig(
                    conversation
            );
            log.info("[websocket] 创建OmniRealtime会话：id={}", conversation.getSessionId());
        } catch (NoApiKeyException e) {
            log.error("[websocket error] 缺少 apikey", e);
        } catch (InterruptedException e) {
            log.error("[websocket error] 线程中断", e);
        }
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
            case START -> {
                log.info("[websocket] 开始录音");
                stopRecording.set(false);
                chatFuture = taskExecutor.submit(() -> {
                    try {
                        omniRealTimeNoVADTestService.startChat(
                                conversation,
                                stopRecording,
                                requestAudioBuffer
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
            case TEXT_MESSAGE -> log.info("[websocket] 收到文本消息：{}", messageMap.get(RealtimeDataTypeEnum.DATA));
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
