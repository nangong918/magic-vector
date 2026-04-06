package com.openapi.connect.websocket.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConnectionManager {

    // userId -> WebSocketSession (Android)
    private final ConcurrentHashMap<String, WebSocketSession> androidSessions = new ConcurrentHashMap<>();
    // deviceId -> WebSocketSession (RK)
    private final ConcurrentHashMap<String, WebSocketSession> rkSessions = new ConcurrentHashMap<>();
    // deviceId -> userId (RK 所属用户)
    private final ConcurrentHashMap<String, String> deviceToUser = new ConcurrentHashMap<>();

    public void registerAndroid(String userId, WebSocketSession session) {
        androidSessions.put(userId, session);
        log.info("Android registered: userId={}", userId);
    }

    public void registerRk(String deviceId, WebSocketSession session, String userId) {
        rkSessions.put(deviceId, session);
        deviceToUser.put(deviceId, userId);
        log.info("RK registered: deviceId={}, userId={}", deviceId, userId);
    }

    public WebSocketSession getAndroidSession(String userId) {
        return androidSessions.get(userId);
    }

    public WebSocketSession getRkSession(String deviceId) {
        return rkSessions.get(deviceId);
    }

    public String getUserIdByDeviceId(String deviceId) {
        return deviceToUser.get(deviceId);
    }

    public void unregisterBySession(WebSocketSession session) {
        androidSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        rkSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        deviceToUser.entrySet().removeIf(entry -> entry.getValue().equals(getUserIdBySession(session)));
    }

    private String getUserIdBySession(WebSocketSession session) {
        for (var entry : androidSessions.entrySet()) {
            if (entry.getValue().equals(session)) return entry.getKey();
        }
        return null;
    }
}