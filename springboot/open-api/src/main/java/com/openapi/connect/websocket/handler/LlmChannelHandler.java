package com.openapi.connect.websocket.handler;

import com.openapi.domain.dto.ws.base.ClientEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
public class LlmChannelHandler implements ChannelHandler {
    @Override
    public void handle(WebSocketSession session, ClientEvent event) {
        log.debug("Handle llm event: sessionId={}, event={}", session.getId(), event.getEvent());
    }
}
