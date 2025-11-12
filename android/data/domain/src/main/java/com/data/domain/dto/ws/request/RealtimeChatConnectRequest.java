package com.data.domain.dto.ws.request;


/**
 * @author 13225
 * @date 2025/10/16 10:38
 */
public class RealtimeChatConnectRequest {
    // 用户Id
    public String userId;
    // agentId(会话id)
    public String agentId;
    // 会话连接时间
    public long timestamp;
}
