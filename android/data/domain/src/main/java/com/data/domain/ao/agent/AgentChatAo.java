package com.data.domain.ao.agent;


import com.data.domain.Do.ChatMessageDo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/10/15 11:25
 */
public class AgentChatAo {
    public AgentAo agentAo;
    public LocalDateTime lastChatTime = LocalDateTime.MIN;
    // 最新的20条消息
    public List<ChatMessageDo> lastChatMessages = new ArrayList<>();
}
