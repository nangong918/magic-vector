package com.openapi.domain.Do;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;


@Data
public class ChatMessageDo {
    @Id
    private String id = String.valueOf(IdUtil.getSnowflake().nextId());
    private String agentId; // 也可也理解为sessionId, 毕竟救我一个用户
//    private String userId; // 就老子一个用户
    private String content;
    // 时间
    private LocalDateTime chatTime;
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    private Integer role = 0;
}
