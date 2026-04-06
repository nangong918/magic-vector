package com.openapi.connect.websocket.handler.base;

import com.google.gson.Gson;
import com.openapi.connect.websocket.manager.ConnectionManager;
import com.openapi.connect.websocket.router.MessageRouter;
import com.openapi.domain.constant.ws.WsChannel;
import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedWsHandler extends TextWebSocketHandler {

    private final MessageRouter messageRouter;
    private final ConnectionManager connectionManager;
    private final Gson gson;// 注入 Gson bean

    private final Map<String, WebSocketSession> pendingAuthSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        pendingAuthSessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) {
        try {
            ClientEvent event = gson.fromJson(message.getPayload(), ClientEvent.class);
            messageRouter.route(session, event);
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        connectionManager.unregisterBySession(session);
        pendingAuthSessions.remove(session.getId());
    }

    private void sendError(WebSocketSession session, String msg) {
        // 构造错误事件，使用 Gson 序列化后发送
        Map<String, String> data = new HashMap<>();
        data.put("code", "INVALID_MESSAGE");
        data.put("message", msg);
        ServerEvent errorEvent = new ServerEvent(WsChannel.SYSTEM.getValue(), WsEvent.ERROR.getValue(), data);
        String json = gson.toJson(errorEvent);
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send error message", e);
        }
    }

}
