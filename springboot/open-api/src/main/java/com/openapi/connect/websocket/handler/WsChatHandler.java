package com.openapi.connect.websocket.handler;

import com.openapi.component.manager.connect.WebSocketConnection;
import com.openapi.component.manager.connect.WebSocketMessage;
import com.openapi.component.manager.realTimeChat.PersistentConnectionManager;
import com.openapi.interfaces.connect.ConnectionSession;
import com.openapi.interfaces.connect.Message;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


/**
 * @author 13225
 * @date 2025/11/13 16:19
 * WsChatHandler, 一个user一个连接
 */
@Slf4j
public class WsChatHandler extends TextWebSocketHandler {

    private final PersistentConnectionManager connectionManager;

    public WsChatHandler(
            PersistentConnectionManager connectionManager
    ){
        this.connectionManager = connectionManager;
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        ConnectionSession connection = new WebSocketConnection(session);
        connectionManager.connect(connection);
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable throwable) throws Exception {
        super.handleTransportError(session, throwable);
        connectionManager.onThrowable(throwable);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String payload = message.getPayload();
        if (!payload.isEmpty()){
            Message websocketMessage = new WebSocketMessage(payload);
            connectionManager.onMessage(websocketMessage);
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        connectionManager.disconnect();
    }


    @Override
    protected void handleBinaryMessage(@NotNull WebSocketSession session, @NotNull BinaryMessage message) {
        super.handleBinaryMessage(session, message);
        byte[] bytes = message.getPayload().array();
        if (bytes.length == 0){
            return;
        }
        log.info("[websocket] 收到二进制消息：id={}，message.length={}", session.getId(), bytes.length);
    }
}
