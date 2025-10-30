package com.data.domain.dto.ws.reponse;


import com.data.domain.constant.chat.RealtimeSystemEventEnum;

/**
 * @author 13225
 * @date 2025/10/29 14:29
 */

public class SystemTextResponse {
    // agentId也相当于会话Id
    public String agentId;
    // userId
    public String userId;
    // 消息Id
    public String messageId;
    /**
     * event
     * @see RealtimeSystemEventEnum
     */
    public String event = RealtimeSystemEventEnum.NULL.getCode();
}
