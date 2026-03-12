package com.openapi.service;

import com.openapi.domain.dto.request.ControlCommandRequest;
import com.openapi.domain.dto.resonse.ControlCommandResponse;
import com.openapi.domain.dto.resonse.ControlStatusResponse;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

public interface ControlConsoleService {
    ControlStatusResponse getControlStatus(String deviceId);

    ControlCommandResponse dispatchCommand(ControlCommandRequest request);

    void onWsConnect(WebSocketSession session, String clientType, String deviceId);

    void onWsMessage(WebSocketSession session, String clientType, String deviceId, Map<String, String> payload);

    void onWsDisconnect(String clientType, String deviceId, String sessionId);
}
