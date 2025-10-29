package com.openapi.domain.dto.ws;

import com.openapi.domain.constant.realtime.RealtimeSystemEventEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 13225
 * @date 2025/10/29 14:29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemTextResponse {
    // agentId也相当于会话Id
    public String agentId;
    // userId
    public String userId;
    // 消息Id
    public String messageId;
    /**
     * event
     * @see com.openapi.domain.constant.realtime.RealtimeSystemEventEnum
     */
    public String event = RealtimeSystemEventEnum.NULL.getCode();
}
