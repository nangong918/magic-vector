package com.openapi.udp;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 13225
 * @date 2025/11/13 12:56
 */
@Data
public class VideoSession {
    private final String userId;
    private final String agentId;
    private final String sessionId;
    private final int totalChunks;
    private final String[] chunks;
    private final AtomicInteger receivedCount = new AtomicInteger(0);

    public VideoSession(String userId, String agentId, String sessionId, int totalChunks) {
        this.userId = userId;
        this.agentId = agentId;
        this.sessionId = sessionId;
        this.totalChunks = totalChunks;
        this.chunks = new String[totalChunks];
    }

    public synchronized void addChunk(int index, String data) {
        if (index >= 0 && index < totalChunks && chunks[index] == null) {
            chunks[index] = data;
            receivedCount.incrementAndGet();
        }
    }

    public boolean isComplete() {
        return receivedCount.get() == totalChunks;
    }

    public String getCompleteData() {
        StringBuilder sb = new StringBuilder();
        for (String chunk : chunks) {
            if (chunk != null) {
                sb.append(chunk);
            }
        }
        return sb.toString();
    }
}
