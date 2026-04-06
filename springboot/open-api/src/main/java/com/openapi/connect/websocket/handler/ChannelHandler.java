package com.openapi.connect.websocket.handler;

import com.openapi.domain.dto.ws.base.ClientEvent;
import org.springframework.web.socket.WebSocketSession;

public interface ChannelHandler {
    void handle(WebSocketSession session, ClientEvent event);
}
