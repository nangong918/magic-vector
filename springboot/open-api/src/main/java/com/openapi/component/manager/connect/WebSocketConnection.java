package com.openapi.component.manager.connect;

import com.openapi.interfaces.connect.ConnectionSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @author 13225
 * @date 2025/11/13 16:25
 */
@Slf4j
public class WebSocketConnection implements ConnectionSession {
    private final WebSocketSession webSocketSession;

    public WebSocketConnection(WebSocketSession webSocketSession){
        this.webSocketSession = webSocketSession;
    }

    @Override
    public void send(String payload) {
        if (StringUtils.hasText(payload)) {
            try {
                webSocketSession.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.error("[WebSocketConnection] send message error", e);
            }
        }
        else {
            log.error("[WebSocketConnection] payload is null");
        }
    }

    @Override
    public String getSessionId() {
        return webSocketSession.getId();
    }

    @Override
    public boolean isConnected() {
        return webSocketSession.isOpen();
    }

    @Override
    public void close() {
        try {
            webSocketSession.close();
        } catch (IOException e) {
            log.error("[WebSocketConnection] close error", e);
        }
    }
}
