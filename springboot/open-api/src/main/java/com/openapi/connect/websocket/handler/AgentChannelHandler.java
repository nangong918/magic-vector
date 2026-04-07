package com.openapi.connect.websocket.handler;

import com.google.gson.Gson;
import com.openapi.connect.websocket.manager.ConnectionManager;
import com.openapi.connect.websocket.manager.WsMessageSenderManager;
import com.openapi.converter.AgentHttpConverter;
import com.openapi.domain.ao.AgentChatAo;
import com.openapi.domain.constant.ws.WsChannel;
import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.dto.http.resonse.AgentChatDto;
import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import com.openapi.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AgentChannelHandler implements ChannelHandler {

    private final AgentService agentService;
    private final AgentHttpConverter agentHttpConverter;
    private final ConnectionManager connectionManager;
    private final Gson gson;
    private final WsMessageSenderManager wsMessageSender;

    @Override
    public void handle(WebSocketSession session, ClientEvent event) {
        WsEvent wsEvent = WsEvent.fromValue(event.getEvent());
        if (wsEvent == WsEvent.NONE) {
            log.warn("Unknown agent event: {}", event.getEvent());
            return;
        }

        if (wsEvent == WsEvent.AGENT_UPDATE) {
            handleAgentUpdate(event);
            return;
        }
        log.debug("Ignore unsupported agent event: {}", wsEvent.getValue());
    }

    private void handleAgentUpdate(ClientEvent event) {
        AgentChatDto request = gson.fromJson(gson.toJson(event.getData()), AgentChatDto.class);
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            log.warn("Invalid agent_update payload");
            return;
        }
        broadcastAgentList(request.getUserId());
    }

    private void broadcastAgentList(String userId) {
        Long userIdLong = parseLong(userId);
        if (userIdLong == null) {
            log.warn("Invalid userId for agent_list_sync: {}", userId);
            return;
        }
        List<AgentChatAo> agentChatAos = agentService.getLastAgentChatList(userIdLong);
        List<AgentChatDto> agentList = agentChatAos.stream().map(agentHttpConverter::chatAoToDto).toList();
        WebSocketSession ownerSession = connectionManager.getAndroidSession(userId);
        boolean online = ownerSession != null && ownerSession.isOpen();
        agentList.forEach(agent -> agent.setOnline(online));
        WebSocketSession androidSession = connectionManager.getAndroidSession(userId);
        if (androidSession == null || !androidSession.isOpen()) {
            return;
        }
        ServerEvent serverEvent = new ServerEvent(
                WsChannel.AGENT.getValue(),
                WsEvent.AGENT_LIST_SYNC.getValue(),
                Map.of("agentList", gson.toJson(agentList))
        );
        wsMessageSender.send(androidSession, serverEvent);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignore) {
            return null;
        }
    }
}
