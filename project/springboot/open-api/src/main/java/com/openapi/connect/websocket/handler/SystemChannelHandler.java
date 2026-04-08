package com.openapi.connect.websocket.handler;

import com.google.gson.Gson;
import com.openapi.connect.websocket.service.WsConversationService;
import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.request.InstructionResultRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@RequiredArgsConstructor
public class SystemChannelHandler implements ChannelHandler {

    private final WsConversationService wsConversationService;
    private final Gson gson;

    @Override
    public void handle(WebSocketSession session, ClientEvent event) {
        WsEvent wsEvent = WsEvent.fromValue(event.getEvent());
        if (wsEvent == WsEvent.INSTRUCTION_EVENT_RESULT) {
            InstructionResultRequest request = gson.fromJson(gson.toJson(event.getData()), InstructionResultRequest.class);
            wsConversationService.handleInstructionResult(session, request);
            return;
        }
        log.debug("Handle system event: sessionId={}, event={}", session.getId(), event.getEvent());
    }
}
