package com.openapi.domain.ao;

import com.openapi.domain.Do.ChatMessageDo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/10/15 11:25
 */
@Data
public class AgentChatAo {
    private AgentAo agentAo;
    private LocalDateTime lastChatTime = LocalDateTime.MIN;
    // 最新的20条消息
    private List<ChatMessageDo> lastChatMessages = new ArrayList<>();
}
