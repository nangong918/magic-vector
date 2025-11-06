package com.openapi.domain.Do;

import com.openapi.component.manager.realTimeChat.RealtimeChatContextManager;
import com.openapi.domain.constant.RoleTypeEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

/**
 * @see RealtimeChatContextManager
 */
@Data
public class ChatMessageDo {
    @Id
    private String id; // 消息Id, 不自增，由RealtimeChatContextManager控制
    private String agentId; // 也可也理解为sessionId
    private String userId;
    private String content;
    // 时间(展示用)
    private LocalDateTime chatTime;
    // 时间(排序用)
    private Long chatTimestamp = 0L;
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    private Integer role = RoleTypeEnum.AGENT.getValue();
}
