package com.openapi.domain.ao;

import com.openapi.domain.Do.ChatMessageDo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/10/15 11:25
 */
@Data
public class AgentChatAo {
    private AgentAo agentAo;
    private Long lastChatTime = 0L;
    // 最新的10条消息
    private List<ChatMessageDo> lastChatMessages = new ArrayList<>();
    // 未读消息 todo 待实现
    private Integer unreadCount = 0;
}
