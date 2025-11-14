package com.openapi.domain.dto.udp;

import com.alibaba.fastjson.JSON;
import lombok.Data;

/**
 * @author 13225
 * @date 2025/11/13 12:53
 */
@Data
public class VideoUdpPacket {
    private String userId;        // 用户ID
    private String agentId;       // 代理ID
//    private String sessionId;     // 会话ID
    private int chunkIndex;       // 分块索引
    private int totalChunks;      // 总块数
    private long timestamp;       // 时间戳
    private String data;          // 实际数据

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public static VideoUdpPacket fromJson(String json) {
        return JSON.parseObject(json, VideoUdpPacket.class);
    }
}
