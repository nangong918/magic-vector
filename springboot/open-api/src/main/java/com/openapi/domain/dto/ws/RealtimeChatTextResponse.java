package com.openapi.domain.dto.ws;

import lombok.Data;

@Data
public class RealtimeChatTextResponse {
    // agentId也相当于会话Id
    public String agentId;
    // 内容
    public String content;
    // 消息Id
    public String messageId;
    // timestamp: 消息发送的时间
    public Long timestamp;
}
