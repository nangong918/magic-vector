package com.openapi.connect.websocket.handler;

import com.google.gson.Gson;
import com.openapi.connect.websocket.manager.ConnectionManager;
import com.openapi.domain.constant.ws.WsChannel;
import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ConnectChannelHandler implements ChannelHandler {

    private final ConnectionManager connectionManager;
    private final Gson gson;

    @Override
    public void handle(WebSocketSession session, ClientEvent event) {
        // 将字符串 event 转为枚举，若未知则忽略
        WsEvent wsEvent = WsEvent.fromValue(event.getEvent());
        if (wsEvent == WsEvent.NONE) {
            log.warn("Unknown connection event: {}", event.getEvent());
            return;
        }

        // 使用 JDK 21 switch 表达式处理不同事件
        switch (wsEvent) {
            case CONNECT -> handleConnect(session, event);
            case RK_CONNECT -> handleRkConnect(session, event);
            case PING -> handlePing(session);
            case PONG -> handlePong(session);
            default -> log.warn("Unhandled connection event: {}", wsEvent);
        }
    }

    private void handleConnect(WebSocketSession session, ClientEvent event) {
        String userId = event.getData().get("userId");
        if (userId == null || userId.isEmpty()) {
            sendAck(session, WsEvent.CONNECT_ACK, 400, "Missing userId");
            return;
        }
        connectionManager.registerAndroid(userId, session);
        sendAck(session, WsEvent.CONNECT_ACK, 200, "OK");
    }

    private void handleRkConnect(WebSocketSession session, ClientEvent event) {
        String deviceId = event.getData().get("deviceId");
        String userId = event.getData().get("userId");
        if (deviceId == null || userId == null) {
            sendAck(session, WsEvent.RK_CONNECT_ACK, 400, "Missing deviceId or userId");
            return;
        }
        connectionManager.registerRk(deviceId, session, userId);
        sendAck(session, WsEvent.RK_CONNECT_ACK, 200, "OK");
    }

    private void handlePing(WebSocketSession session) {
        try {
            ServerEvent pong = new ServerEvent(
                    WsChannel.CONNECTION.getValue(),
                    WsEvent.PONG.getValue(),
                    Map.of()
            );
            session.sendMessage(new TextMessage(gson.toJson(pong)));
        } catch (IOException e) {
            log.error("Failed to send pong", e);
        }
    }

    private void handlePong(WebSocketSession session) {
        // 可选：更新心跳时间戳等
        log.debug("Received pong from session {}", session.getId());
    }

    private void sendAck(WebSocketSession session, WsEvent ackEvent, int code, String message) {
        try {
            ServerEvent ack = new ServerEvent(
                    WsChannel.CONNECTION.getValue(),
                    ackEvent.getValue(),
                    Map.of("code", String.valueOf(code), "message", message)
            );
            session.sendMessage(new TextMessage(gson.toJson(ack)));
        } catch (IOException e) {
            log.error("Failed to send ack", e);
        }
    }
}