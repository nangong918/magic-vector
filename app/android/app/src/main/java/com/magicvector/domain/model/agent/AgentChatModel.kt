package com.magicvector.domain.model.agent;


import com.data.domain.Do.ChatMessageEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/10/15 11:25
 */
public class AgentChatModel {
    public AgentModel agentModel;
    public Long lastChatTime = 0L;
    // 最新的10条消息
    public List<ChatMessageEntity> lastChatMessages = new ArrayList<>();
    // 未读消息
    public Integer unreadCount = 0;
}
