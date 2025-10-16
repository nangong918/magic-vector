package com.openapi.domain.dto.ws;

import com.openapi.domain.constant.RoleTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RealtimeChatTextResponse {
    // agentId也相当于会话Id
    public String agentId;
    // userId
    public String userId;
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    public Integer role = RoleTypeEnum.AGENT.getValue();
    // 内容
    public String content;
    // 消息Id
    public String messageId;
    // timestamp: 消息发送的时间 用于排序
    public Long timestamp;
    // date time：消息发送时间 用于展示
    public LocalDateTime chatTime;
}
