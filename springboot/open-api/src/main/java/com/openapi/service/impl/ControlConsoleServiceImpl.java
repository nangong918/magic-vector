package com.openapi.service.impl;

import com.alibaba.fastjson.JSON;
import com.openapi.component.manager.control.ControlSessionManager;
import com.openapi.domain.dto.request.ControlCommandRequest;
import com.openapi.domain.dto.resonse.ControlCommandResponse;
import com.openapi.domain.dto.resonse.ControlStatusResponse;
import com.openapi.service.ControlAgentLogService;
import com.openapi.service.ControlConsoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlConsoleServiceImpl implements ControlConsoleService {
    private final ControlSessionManager controlSessionManager;
    private final ControlAgentLogService controlAgentLogService;

    @Override
    public ControlStatusResponse getControlStatus(String deviceId) {
        return controlSessionManager.buildStatus(deviceId);
    }

    @Override
    public ControlCommandResponse dispatchCommand(ControlCommandRequest request) {
        ControlCommandResponse response = new ControlCommandResponse();
        response.setTraceId(UUID.randomUUID().toString());
        response.setAccepted(Boolean.FALSE);

        if (request == null || !StringUtils.hasText(request.getDeviceId())) {
            response.setMessage("invalid request");
            return response;
        }

        // TODO-RK-MQTT: 后续可增加 MQTT 下行通道。
        // 当前优先通过 SpringBoot 控制 WS 转发给已连接 RK 设备。
        String wsPayload = JSON.toJSONString(Map.of(
                "type", "COMMAND",
                "traceId", response.getTraceId(),
                "transport", request.getTransport(),
                "commandType", request.getCommandType(),
                "sequence", String.valueOf(request.getSequence()),
                "timestamp", String.valueOf(request.getTimestamp()),
                "payloadJson", request.getPayloadJson()
        ));
        boolean forwarded = controlSessionManager.forwardCommandToRk(request.getDeviceId(), wsPayload);
        response.setAccepted(forwarded);
        response.setMessage(forwarded ? "dispatched by control ws" : "rk is offline, command queued TODO");
        String agentId = parseAgentId(request.getPayloadJson(), request.getDeviceId());
        controlAgentLogService.saveControlLog(
                request.getUserId(),
                agentId,
                wsPayload,
                System.currentTimeMillis()
        );
        return response;
    }

    @Override
    public void onWsConnect(WebSocketSession session, String clientType, String deviceId) {
        controlSessionManager.bindSession(deviceId, clientType, session);
    }

    @Override
    public void onWsMessage(
            WebSocketSession session,
            String clientType,
            String deviceId,
            Map<String, String> payload
    ) {
        String type = payload.getOrDefault("type", "");
        if ("HEARTBEAT".equalsIgnoreCase(type)) {
            controlSessionManager.updateHeartbeat(deviceId);
            return;
        }
        if ("RK_AGENT_MODE".equalsIgnoreCase(type)) {
            controlSessionManager.updateRkAgentMode(deviceId, payload.getOrDefault("rkAgentMode", "TODO_RK_AGENT"));
            return;
        }
        if ("STATUS_SYNC".equalsIgnoreCase(type)) {
            controlSessionManager.updateHeartbeat(deviceId);
            if ("rk".equalsIgnoreCase(clientType)) {
                controlSessionManager.updateRkAgentMode(deviceId, payload.getOrDefault("rkAgentMode", "TODO_RK_AGENT"));
            }
            controlAgentLogService.saveControlLog(
                    payload.getOrDefault("userId", "0"),
                    payload.getOrDefault("agentId", deviceId),
                    JSON.toJSONString(payload),
                    System.currentTimeMillis()
            );
            return;
        }
        log.info("[control] ignore ws message type={}, deviceId={}", type, deviceId);
    }

    @Override
    public void onWsDisconnect(String clientType, String deviceId, String sessionId) {
        controlSessionManager.unbindSession(deviceId, clientType, sessionId);
    }

    private String parseAgentId(String payloadJson, String fallback) {
        try {
            Map<String, String> map = JSON.parseObject(
                    payloadJson,
                    new com.alibaba.fastjson.TypeReference<Map<String, String>>() {
                    }
            );
            if (map != null && StringUtils.hasText(map.get("agentId"))) {
                return map.get("agentId");
            }
        } catch (Exception ignore) {
        }
        return fallback;
    }
}
