package com.data.domain.Do;



import com.data.domain.constant.chat.RoleTypeEnum;

import java.time.LocalDateTime;


public class ChatMessageDo {
    // messageId
    public String id;
    public String agentId;
    public String content;
    // 时间 -> timestamp
    public LocalDateTime chatTime;
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    public Integer role = RoleTypeEnum.AGENT.getValue();
}
