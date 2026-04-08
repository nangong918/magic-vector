package com.openapi.connect.websocket.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WsInstructionTrackerManager {

    /** key=requestId，value=该批次指令的执行进度。 */
    private final ConcurrentHashMap<String, Tracker> trackers = new ConcurrentHashMap<>();

    /** 注册一个新的 instruction_list 批次。 */
    public void register(String requestId, WebSocketSession session, String userId, int totalCount) {
        trackers.put(requestId, new Tracker(requestId, session, userId, totalCount, new AtomicInteger(0)));
    }

    /** 获取批次追踪信息。 */
    public Tracker get(String requestId) {
        return trackers.get(requestId);
    }

    /**
     * 标记一条指令执行完成。
     * @return true 表示该 requestId 的全部指令已完成
     */
    public boolean markDone(String requestId) {
        Tracker tracker = trackers.get(requestId);
        if (tracker == null) {
            return false;
        }
        // 只做计数归并，不做业务判断；业务层根据 true 决定是否结束回复。
        int done = tracker.doneCount.incrementAndGet();
        return done >= tracker.totalCount;
    }

    /** 移除一个批次追踪信息。 */
    public void remove(String requestId) {
        trackers.remove(requestId);
    }

    @Data
    @AllArgsConstructor
    public static class Tracker {
        // instruction_list 请求唯一标识
        private String requestId;
        // 原始会话（用于回推 agent_end_reply）
        private WebSocketSession session;
        private String userId;
        // 本批指令总数
        private int totalCount;
        // 已完成数量（原子计数）
        private AtomicInteger doneCount;
    }
}
