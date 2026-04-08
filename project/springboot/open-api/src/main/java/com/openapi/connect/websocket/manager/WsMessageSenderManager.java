package com.openapi.connect.websocket.manager;

import com.google.gson.Gson;
import com.openapi.domain.dto.ws.base.ServerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsMessageSenderManager {

    private final Gson gson;
    private final WsMessageQueueManager queueManager;

    public void send(WebSocketSession session, ServerEvent event) {
        if (session == null || !session.isOpen() || event == null) {
            return;
        }
        if (!queueManager.enqueueOutbound(session, event)) {
            return;
        }
        flush(session);
    }

    public void flush(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            return;
        }
        synchronized (session) {
            ServerEvent next;
            while ((next = queueManager.pollOutbound(session)) != null) {
                try {
                    session.sendMessage(new TextMessage(gson.toJson(next)));
                } catch (IOException e) {
                    log.error("Send websocket message failed, sessionId={}, event={}", session.getId(), next.getEvent(), e);
                    break;
                }
            }
        }
    }
}
