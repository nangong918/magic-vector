package com.data.domain.Do;



import com.data.domain.constant.chat.RoleTypeEnum;

import java.time.LocalDateTime;


public class ChatMessageDo {
    public String id; // 消息Id, 不自增，由RealtimeChatContextManager控制
    public String agentId; // 也可也理解为sessionId
    public String userId;
    public String content;
    // 时间(展示用)
    public LocalDateTime chatTime;
    // 时间(排序用)
    public Long chatTimestamp = 0L;
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    public Integer role = RoleTypeEnum.AGENT.getValue();
}
