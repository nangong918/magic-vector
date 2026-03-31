package com.openapi.component.manager.control;

import com.openapi.domain.dto.http.resonse.ControlStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 控制台连接会话管理器。
 * 计算机网络说明：一个 deviceId 维护两类通道（app/rk），用于云端桥接转发。
 */
@Slf4j
@Component
public class ControlSessionManager {
    private final Map<String, WebSocketSession> appSessionByDevice = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> rkSessionByDevice = new ConcurrentHashMap<>();
    private final Map<String, String> rkAgentModeByDevice = new ConcurrentHashMap<>();
    private final Map<String, Long> heartbeatByDevice = new ConcurrentHashMap<>();

    public void bindSession(String deviceId, String clientType, WebSocketSession session) {
        if (!StringUtils.hasText(deviceId) || session == null) {
            return;
        }
        if ("rk".equalsIgnoreCase(clientType)) {
            rkSessionByDevice.put(deviceId, session);
        } else {
            appSessionByDevice.put(deviceId, session);
        }
        heartbeatByDevice.put(deviceId, System.currentTimeMillis());
    }

    public void unbindSession(String deviceId, String clientType, String sessionId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        if ("rk".equalsIgnoreCase(clientType)) {
            removeIfSameSession(rkSessionByDevice, deviceId, sessionId);
        } else {
            removeIfSameSession(appSessionByDevice, deviceId, sessionId);
        }
    }

    public void updateHeartbeat(String deviceId) {
        if (StringUtils.hasText(deviceId)) {
            heartbeatByDevice.put(deviceId, System.currentTimeMillis());
        }
    }

    public void updateRkAgentMode(String deviceId, String rkAgentMode) {
        if (StringUtils.hasText(deviceId) && StringUtils.hasText(rkAgentMode)) {
            rkAgentModeByDevice.put(deviceId, rkAgentMode);
        }
    }

    public ControlStatusResponse buildStatus(String deviceId) {
        ControlStatusResponse response = new ControlStatusResponse();
        response.setDeviceId(deviceId);
        response.setAppToSpringConnected(isConnected(appSessionByDevice.get(deviceId)));
        response.setRkToSpringConnected(isConnected(rkSessionByDevice.get(deviceId)));
        response.setAppToRkWifiConnected(Boolean.FALSE);
        response.setAppToRkBleConnected(Boolean.FALSE);
        response.setRkAgentMode(rkAgentModeByDevice.getOrDefault(deviceId, "TODO_RK_AGENT"));
        response.setLastHeartbeatAt(heartbeatByDevice.get(deviceId));
        return response;
    }

    public boolean forwardCommandToRk(String deviceId, String payload) {
        WebSocketSession rkSession = rkSessionByDevice.get(deviceId);
        if (!isConnected(rkSession)) {
            return false;
        }
        try {
            synchronized (rkSession) {
                rkSession.sendMessage(new TextMessage(payload));
            }
            return true;
        } catch (IOException e) {
            log.error("[control] forward rk command error, deviceId={}", deviceId, e);
            return false;
        }
    }

    private boolean isConnected(WebSocketSession session) {
        return session != null && session.isOpen();
    }

    private void removeIfSameSession(Map<String, WebSocketSession> source, String deviceId, String sessionId) {
        WebSocketSession existing = source.get(deviceId);
        if (existing != null && existing.getId().equals(sessionId)) {
            source.remove(deviceId);
        }
    }
}
