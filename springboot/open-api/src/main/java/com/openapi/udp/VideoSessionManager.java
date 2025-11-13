package com.openapi.udp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.openapi.config.SessionConfig;
import com.openapi.domain.dto.udp.VideoUdpPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 13225
 * @date 2025/11/13 14:49
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSessionManager {

    private final SessionConfig sessionConfig;
    private final Map<String, VideoSession> sessions = new ConcurrentHashMap<>();

    public void processVideoPacket(VideoUdpPacket packet) {
        String sessionKey = buildSessionKey(packet.getUserId(), packet.getAgentId(), packet.getSessionId());

        VideoSession session = sessions.computeIfAbsent(sessionKey,
                k -> new VideoSession(packet.getUserId(), packet.getAgentId(),
                        packet.getSessionId(), packet.getTotalChunks()));

        session.addChunk(packet.getChunkIndex(), packet.getData());

        if (session.isComplete()) {
            processCompleteVideo(session, sessionKey);
        }
    }

    private void processCompleteVideo(VideoSession session, String sessionKey) {
        try {
            long startTime = System.currentTimeMillis();
            String completeBase64 = session.getCompleteData();

            log.info("收到完整视频 - 会话: {}, 用户: {}, 数据大小: {} bytes, 耗时: {}ms",
                    session.getSessionId(), session.getUserId(),
                    completeBase64.length(), System.currentTimeMillis() - startTime);

            // 完成之后将数据流交给VLManager
            var vlManager = sessionConfig.vlManagerMap().get(session.getAgentId());
            if (vlManager != null){
                vlManager.videoBase64 = completeBase64;
            }
            else {
                log.warn("[processCompleteVideo] 未找到对应的VLManager - agentId: {}", session.getAgentId());
            }

            // 处理完成后移除会话
            sessions.remove(sessionKey);

        } catch (Exception e) {
            log.error("处理完整视频异常 - 会话: {}", sessionKey, e);
        }
    }

    private String buildSessionKey(String userId, String agentId, String sessionId) {
        return userId + ":" + agentId + ":" + sessionId;
    }

    @PreDestroy
    public void shutdown() {
        sessions.clear();
    }

    // 获取统计信息
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", sessions.size());
        stats.put("sessionKeys", new ArrayList<>(sessions.keySet()));
        return stats;
    }

}
