package com.openapi.domain.dto.ws;

import lombok.Data;

/**
 * @author 13225
 * @date 2025/10/16 10:38
 */
@Data
public class RealtimeChatConnectRequest {
    // 用户Id
    public String userId;
    // agentId(会话id)
    public String agentId;
    // 会话连接时间
    public long timestamp;
}
