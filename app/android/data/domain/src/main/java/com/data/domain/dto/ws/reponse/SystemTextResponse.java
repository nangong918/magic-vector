package com.data.domain.dto.ws.reponse;


import com.data.domain.constant.chat.RealtimeSystemResponseEventEnum;

/**
 * @author 13225
 * @date 2025/10/29 14:29
 */

public class SystemTextResponse {
    // agentId也相当于会话Id
    public String agentId;
    // userId
    public String userId;
    /**
     * event
     * @see RealtimeSystemResponseEventEnum
     */
    public String event = RealtimeSystemResponseEventEnum.NULL.getCode();
}
