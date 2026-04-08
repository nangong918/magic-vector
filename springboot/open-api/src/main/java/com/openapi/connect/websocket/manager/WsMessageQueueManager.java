package com.openapi.connect.websocket.manager;

import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebSocket 收发队列管理：
 * - inbound: sessionId + channel 维度，保证同通道顺序消费；
 * - outbound: session 维度，统一串行发送。
 */
@Slf4j
@Component
public class WsMessageQueueManager {

    /** 单队列容量，和 Android 端文档保持一致。 */
    private static final int QUEUE_CAPACITY = 32;

    /** 入站队列：key=sessionId:channel，保证同 session 同通道顺序消费。 */
    private final ConcurrentHashMap<String, BlockingQueue<ClientEvent>> inboundQueues = new ConcurrentHashMap<>();
    /** 出站队列：key=sessionId，统一出站排队发送。 */
    private final ConcurrentHashMap<String, BlockingQueue<ServerEvent>> outboundQueues = new ConcurrentHashMap<>();

    /** 建立连接时初始化出站队列（入站按需初始化）。 */
    public void registerSession(WebSocketSession session) {
        outboundQueues.computeIfAbsent(session.getId(), key -> new LinkedBlockingQueue<>(QUEUE_CAPACITY));
    }

    /** 连接关闭时清理该 session 的所有队列。 */
    public void unregisterSession(WebSocketSession session) {
        String inboundPrefix = session.getId() + ":";
        inboundQueues.keySet().removeIf(key -> key.startsWith(inboundPrefix));
        outboundQueues.remove(session.getId());
    }

    /** 入站入队：返回 false 表示队列已满。 */
    public boolean enqueueInbound(WebSocketSession session, @NonNull ClientEvent event) {
        // 用 channel 维度隔离 inbound，避免不同 handler 竞争同一队列。
        String inboundKey = buildInboundQueueKey(session.getId(), event.getChannel());
        BlockingQueue<ClientEvent> queue = inboundQueues.computeIfAbsent(
                inboundKey, key -> new LinkedBlockingQueue<>(QUEUE_CAPACITY)
        );
        boolean offered = queue.offer(event);
        if (!offered) {
            log.warn("Inbound queue full, sessionId={}, channel={}, event={}",
                    session.getId(),
                    event.getChannel(),
                    event.getEvent());
        }
        return offered;
    }

    /** 入站出队：按 session + channel 拉取一条事件。 */
    public ClientEvent pollInbound(WebSocketSession session, String channel) {
        BlockingQueue<ClientEvent> queue = inboundQueues.get(buildInboundQueueKey(session.getId(), channel));
        if (queue == null) {
            return null;
        }
        return queue.poll();
    }

    /** 出站入队：返回 false 表示队列已满。 */
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

    /** 出站出队：按 session 拉取一条待发送消息。 */
    public ServerEvent pollOutbound(WebSocketSession session) {
        BlockingQueue<ServerEvent> queue = outboundQueues.get(session.getId());
        if (queue == null) {
            return null;
        }
        return queue.poll();
    }

    /** 统一生成入站队列 key。 */
    private String buildInboundQueueKey(String sessionId, String channel) {
        return sessionId + ":" + (channel == null ? "none" : channel);
    }
}
