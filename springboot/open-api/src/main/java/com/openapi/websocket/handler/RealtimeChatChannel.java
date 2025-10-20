package com.openapi.websocket.handler;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openapi.component.manager.RealtimeChatContextManager;
import com.openapi.domain.constant.realtime.RealtimeRequestDataTypeEnum;
import com.openapi.domain.constant.realtime.RealtimeResponseDataTypeEnum;
import com.openapi.domain.dto.ws.RealtimeChatConnectRequest;
import com.openapi.service.RealtimeChatService;
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
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
@Component
public class RealtimeChatChannel extends TextWebSocketHandler {

    private final ThreadPoolTaskExecutor taskExecutor;
    private volatile Future<?> chatFuture;
    private RealtimeChatContextManager realtimeChatContextManager = new RealtimeChatContextManager();
    private final RealtimeChatService realtimeChatService;
    private final DashScopeChatModel dashScopeChatModel;
    private ChatClient chatClient;


    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        log.info("[websocket] 创建连接：id={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        log.info("[websocket] 连接断开：id={}，reason={}", session.getId(), status);
        realtimeChatContextManager.stopRecording.set(true);
        // 取消聊天任务
        if (chatFuture != null) {
            // true 表示中断正在执行的任务
            chatFuture.cancel(true);
        }
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, @NotNull Throwable exception) throws Exception {
        log.error("[websocket] 连接异常：id={}", session.getId(), exception);
//        session.close();
        realtimeChatContextManager.stopRecording.set(true);
        super.handleTransportError(session, exception);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws Exception {

        Map<String, String> messageMap = JSON.parseObject(message.getPayload(), new TypeReference<>() {});
        String type = messageMap.get(RealtimeRequestDataTypeEnum.TYPE);
        if (!StringUtils.hasText(type)) {
            log.warn("[websocket warn] 收到消息，类型为空");
            return;
        }

        RealtimeRequestDataTypeEnum realtimeDataTypeEnum = RealtimeRequestDataTypeEnum.getByType(type);
        switch (realtimeDataTypeEnum) {
            case CONNECT -> {
                log.info("[websocket] 连接，收集用户会话信息");
                String connectMessage = messageMap.get(RealtimeRequestDataTypeEnum.DATA);
                try {
                    RealtimeChatConnectRequest connectRequest = JSON.parseObject(connectMessage, RealtimeChatConnectRequest.class);
                    // 重新连接就是新的会话信息
                    realtimeChatContextManager = new RealtimeChatContextManager();
                    realtimeChatContextManager.userId = connectRequest.getUserId();
                    realtimeChatContextManager.agentId = connectRequest.getAgentId();
                    realtimeChatContextManager.session = session;
                    realtimeChatContextManager.connectTimestamp = connectRequest.getTimestamp();

                    // 初始化chatClient
                    chatClient = realtimeChatService.initChatClient(realtimeChatContextManager, dashScopeChatModel);
                } catch (Exception e){
                    log.warn("[websocket warn] 断开连接，参数错误");
//                    session.close();
                }
            }
            case START_AUDIO_RECORD -> {
                // 无VAD模式
                // 开启新的一问一答
                realtimeChatContextManager.newChatMessage();
                log.info("[websocket] 开始录音");
                realtimeChatContextManager.stopRecording.set(false);
                chatFuture = taskExecutor.submit(() -> {
                    try {
                        // 启动聊天
                        realtimeChatService.startChat(
                                realtimeChatContextManager,
                                chatClient
                        );
                    } catch (Exception e) {
                        realtimeChatContextManager.stopRecording.set(true);
                        log.error("[audioChat] 聊天处理异常", e);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                        responseMap.put(RealtimeResponseDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException ex) {
                            log.error("[websocket error] 响应消息异常", ex);
                        }
                    }
                });
            }
            case STOP_AUDIO_RECORD -> {
                log.info("[websocket] 停止录音");
                realtimeChatContextManager.stopRecording.set(true);
            }
            case AUDIO_CHUNK -> {
                log.info("[websocket] 收到音频块");
                handleAudioChunk(messageMap.get(RealtimeRequestDataTypeEnum.DATA));
            }
            case USER_TEXT_MESSAGE -> {
                log.info("[websocket] 收到文本消息：{}", messageMap.get(RealtimeRequestDataTypeEnum.DATA));
                realtimeChatContextManager.newChatMessage();
                realtimeChatContextManager.stopRecording.set(false);
                chatFuture = taskExecutor.submit(() -> {
                    try {
                        // 启动text聊天
                        realtimeChatService.startTextChat(
                                messageMap.get(RealtimeRequestDataTypeEnum.DATA),
                                realtimeChatContextManager,
                                chatClient
                        );
                    } catch (Exception e) {
                        realtimeChatContextManager.stopRecording.set(true);
                        log.error("[audioChat] 聊天处理异常", e);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                        responseMap.put(RealtimeResponseDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
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
        realtimeChatContextManager.requestAudioBuffer.offer(audioBytes);
    }

}
