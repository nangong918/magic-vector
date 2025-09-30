package com.data.domain.Do;



import java.time.LocalDateTime;


public class ChatMessageDo {
    public String id;
    public String agentId; // 也可也理解为sessionId, 毕竟救我一个用户
    public String content;
    // 时间
    public LocalDateTime chatTime;
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    public Integer role = 0;
}
