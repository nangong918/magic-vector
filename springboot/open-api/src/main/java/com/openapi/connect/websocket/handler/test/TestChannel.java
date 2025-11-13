package com.openapi.connect.websocket.handler.test;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;


@Slf4j
@Component
public class TestChannel extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("[websocket] 新的连接：id={}", session.getId());
        super.afterConnectionEstablished(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("[websocket] 收到消息：id={}，message={}", session.getId(), message.getPayload());

        if (message.getPayload().equalsIgnoreCase("bye")) {
            session.close(CloseStatus.NORMAL);
            return;
        }

        String response = "[" + Instant.now().toEpochMilli() + "] Hello " + message.getPayload();
        session.sendMessage(new TextMessage(response));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.info("[websocket] 连接异常：id={}，throwable={}", session.getId(), exception.getMessage());
        session.close();
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        log.info("[websocket] 连接断开：id={}，reason={}", session.getId(), status);
        super.afterConnectionClosed(session, status);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false; // 如果不支持部分消息，返回 false
    }

}
