package com.demo.webrtc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC 信令中转器：
 * - 客户端通过 ws://host/ws/webrtc?uid=xxx 建立连接并绑定 ID；
 * - 服务端仅负责转发呼叫、SDP、ICE 等消息，不参与媒体流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebRtcSignalingHandler extends TextWebSocketHandler {
    private static final Set<String> RELAY_TYPES = Set.of(
            "call_invite",
            "call_accept",
            "call_reject",
            "call_joined",
            "offer",
            "answer",
            "ice_candidate",
            "hangup"
    );

    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> uidToSession = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdToUid = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uid = extractUid(session.getUri());
        if (!StringUtils.hasText(uid)) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        WebSocketSession oldSession = uidToSession.put(uid, session);
        sessionIdToUid.put(session.getId(), uid);

        if (oldSession != null && oldSession.isOpen() && !oldSession.getId().equals(session.getId())) {
            sendError(oldSession, "duplicated_uid", "该 ID 在其他设备登录，当前连接已替换旧连接");
            oldSession.close(CloseStatus.NORMAL.withReason("replaced_by_new_session"));
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("selfId", uid);
        payload.put("onlineCount", uidToSession.size());
        sendSystemMessage(session, "bind_ok", uid, payload);
        log.info("[webrtc] bind success uid={}, sessionId={}", uid, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String fromUid = sessionIdToUid.get(session.getId());
        if (!StringUtils.hasText(fromUid)) {
            sendError(session, "not_bound", "连接未绑定 uid");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = textValue(root, "type");
            if (!StringUtils.hasText(type)) {
                sendError(session, "bad_request", "type 不能为空");
                return;
            }
            if ("ping".equals(type)) {
                sendSystemMessage(session, "pong", fromUid, null);
                return;
            }
            if (!RELAY_TYPES.contains(type)) {
                sendError(session, "unsupported_type", "不支持的消息类型: " + type);
                return;
            }
            String toUid = textValue(root, "to");
            if (!StringUtils.hasText(toUid)) {
                sendError(session, "bad_request", "to 不能为空");
                return;
            }
            String callId = textValue(root, "callId");
            JsonNode payload = root.get("payload");
            forwardMessage(type, fromUid, toUid, callId, payload);
        } catch (Exception e) {
            log.warn("[webrtc] handle message failed, uid={}, payload={}", fromUid, message.getPayload(), e);
            sendError(session, "bad_json", "消息解析失败");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("[webrtc] transport error, sessionId={}", session.getId(), exception);
        cleanupSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanupSession(session);
        log.info("[webrtc] session closed, sessionId={}, status={}", session.getId(), status);
    }

    private void forwardMessage(String type, String fromUid, String toUid, String callId, JsonNode payload) {
        WebSocketSession toSession = uidToSession.get(toUid);
        if (toSession == null || !toSession.isOpen()) {
            WebSocketSession fromSession = uidToSession.get(fromUid);
            if (fromSession != null && fromSession.isOpen()) {
                sendError(fromSession, "user_offline", "目标用户不在线: " + toUid);
            }
            log.info("[webrtc] relay failed type={}, from={}, to={}, callId={}, reason=user_offline", type, fromUid, toUid, callId);
            return;
        }
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("type", type);
        envelope.put("from", fromUid);
        envelope.put("to", toUid);
        if (StringUtils.hasText(callId)) {
            envelope.put("callId", callId);
        }
        if (payload != null && !payload.isNull()) {
            envelope.set("payload", payload);
        }
        sendMessage(toSession, envelope);
        log.info("[webrtc] relay success type={}, from={}, to={}, callId={}", type, fromUid, toUid, callId);
    }

    private void sendSystemMessage(WebSocketSession session, String type, String selfId, JsonNode payload) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("type", type);
        if (StringUtils.hasText(selfId)) {
            message.put("selfId", selfId);
        }
        if (payload != null && !payload.isNull()) {
            message.set("payload", payload);
        }
        sendMessage(session, message);
    }

    private void sendError(WebSocketSession session, String code, String message) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("code", code);
        payload.put("message", message);
        sendSystemMessage(session, "error", null, payload);
    }

    private void sendMessage(WebSocketSession session, ObjectNode data) {
        try {
            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
        } catch (IOException e) {
            log.warn("[webrtc] send message failed, sessionId={}", session.getId(), e);
        }
    }

    private String extractUid(URI uri) {
        if (uri == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("uid");
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        return value == null ? null : value.asText();
    }

    private void cleanupSession(WebSocketSession session) {
        String uid = sessionIdToUid.remove(session.getId());
        if (!StringUtils.hasText(uid)) {
            return;
        }
        uidToSession.computeIfPresent(uid, (key, value) ->
                value.getId().equals(session.getId()) ? null : value);
    }
}
