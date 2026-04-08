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
public class InstructionListChannelHandler implements ChannelHandler {

    private final WsConversationService wsConversationService;
    private final Gson gson;

    @Override
    public void handle(WebSocketSession session, ClientEvent event) {
        WsEvent wsEvent = WsEvent.fromValue(event.getEvent());
        if (wsEvent != WsEvent.INSTRUCTION_RESULT) {
            log.debug("Ignore instruction_list event: {}", event.getEvent());
            return;
        }
        // 客户端仅上报单条执行回执，由会话编排器统一汇总。
        InstructionResultRequest request = gson.fromJson(gson.toJson(event.getData()), InstructionResultRequest.class);
        wsConversationService.handleInstructionResult(session, request);
    }
}
