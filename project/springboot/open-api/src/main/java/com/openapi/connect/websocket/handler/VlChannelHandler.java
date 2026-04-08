package com.openapi.connect.websocket.handler;

import com.google.gson.Gson;
import com.openapi.connect.websocket.manager.WsAgentSyncManager;
import com.openapi.domain.constant.ws.WsEvent;
import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.request.VlStartRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

/**
 * VL 通道：当前客户端上行仅 {@code vl_start}（见 WS 文档）；VL 文本由 UDP/RTMP 等内部链路回调
 * {@link WsAgentSyncManager#notifyVlPipelineComplete} 注入，与 STT 最终在 {@link WsAgentSyncManager#finishSttAndSyncVl} 合并。
 */
@Slf4j
@RequiredArgsConstructor
public class VlChannelHandler implements ChannelHandler {

    private final WsAgentSyncManager wsAgentSyncManager;
    private final Gson gson;

    @Override
    public void handle(WebSocketSession session, ClientEvent event) {
        WsEvent wsEvent = WsEvent.fromValue(event.getEvent());
        if (wsEvent == WsEvent.VL_START) {
            handleVlStart(session, event);
            return;
        }
        log.debug("Ignore vl event (no inbound handler): sessionId={}, event={}", session.getId(), event.getEvent());
    }

    private void handleVlStart(WebSocketSession session, ClientEvent event) {
        VlStartRequest request = gson.fromJson(gson.toJson(event.getData()), VlStartRequest.class);
        if (request == null || request.getAgentId() == null || request.getAgentId().isBlank()) {
            return;
        }
        wsAgentSyncManager.onClientVlStart(session.getId(), request.getAgentId());
    }
}
