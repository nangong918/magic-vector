package com.openapi.domain.dto.ws.request;

import lombok.Data;

/**
 * @author 13225
 * @date 2025/10/16 10:38
 */
@Data
public class RealtimeChatConnectRequest {
    // 用户Id
    public String userId;
    // 会话连接时间
    public long timestamp;
}
