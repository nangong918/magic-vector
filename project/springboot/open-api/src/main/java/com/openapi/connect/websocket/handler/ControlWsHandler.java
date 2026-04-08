package com.openapi.connect.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openapi.service.ControlConsoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ControlWsHandler extends TextWebSocketHandler {
    private final ControlConsoleService controlConsoleService;

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        String clientType = getClientType(session);
        String deviceId = getDeviceId(session);
        if (!StringUtils.hasText(deviceId)) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        controlConsoleService.onWsConnect(session, clientType, deviceId);
        log.info("[control ws] connected, sessionId={}, clientType={}, deviceId={}",
                session.getId(), clientType, deviceId);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String payload = message.getPayload();
        Map<String, String> data = JSON.parseObject(payload, new TypeReference<>() {});
        controlConsoleService.onWsMessage(
                session,
                getClientType(session),
                getDeviceId(session),
                data == null ? Map.of() : data
        );
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        controlConsoleService.onWsDisconnect(
                getClientType(session),
                getDeviceId(session),
                session.getId()
        );
    }

    private String getClientType(WebSocketSession session) {
        String value = session.getUri() == null ? null : extractQueryValue(session.getUri().getQuery(), "clientType");
        return StringUtils.hasText(value) ? value : "app";
    }

    private String getDeviceId(WebSocketSession session) {
        return session.getUri() == null ? "" : extractQueryValue(session.getUri().getQuery(), "deviceId");
    }

    private String extractQueryValue(String query, String key) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(key)) {
            return "";
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return kv[1];
            }
        }
        return "";
    }
}
