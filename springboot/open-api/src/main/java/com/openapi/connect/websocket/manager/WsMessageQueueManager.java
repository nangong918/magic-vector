package com.openapi.connect.websocket.manager;

import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebSocket消息队列管理
 */
@Slf4j
@Component
public class WsMessageQueueManager {

    private static final int QUEUE_CAPACITY = 32;

    private final ConcurrentHashMap<String, BlockingQueue<ClientEvent>> inboundQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<ServerEvent>> outboundQueues = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session) {
        inboundQueues.computeIfAbsent(session.getId(), key -> new LinkedBlockingQueue<>(QUEUE_CAPACITY));
        outboundQueues.computeIfAbsent(session.getId(), key -> new LinkedBlockingQueue<>(QUEUE_CAPACITY));
    }

    public void unregisterSession(WebSocketSession session) {
        inboundQueues.remove(session.getId());
        outboundQueues.remove(session.getId());
    }

    public boolean enqueueInbound(WebSocketSession session, ClientEvent event) {
        BlockingQueue<ClientEvent> queue = inboundQueues.computeIfAbsent(
                session.getId(), key -> new LinkedBlockingQueue<>(QUEUE_CAPACITY)
        );
        boolean offered = queue.offer(event);
        if (!offered) {
            log.warn("Inbound queue full, sessionId={}, event={}", session.getId(), event.getEvent());
        }
        return offered;
    }

    public ClientEvent pollInbound(WebSocketSession session) {
        BlockingQueue<ClientEvent> queue = inboundQueues.get(session.getId());
        if (queue == null) {
            return null;
        }
        return queue.poll();
    }

    public boolean enqueueOutbound(WebSocketSession session, ServerEvent event) {
        BlockingQueue<ServerEvent> queue = outboundQueues.computeIfAbsent(
                session.getId(), key -> new LinkedBlockingQueue<>(QUEUE_CAPACITY)
        );
        boolean offered = queue.offer(event);
        if (!offered) {
            log.warn("Outbound queue full, sessionId={}, event={}", session.getId(), event.getEvent());
        }
        return offered;
    }

    public ServerEvent pollOutbound(WebSocketSession session) {
        BlockingQueue<ServerEvent> queue = outboundQueues.get(session.getId());
        if (queue == null) {
            return null;
        }
        return queue.poll();
    }
}
