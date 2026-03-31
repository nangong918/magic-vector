package com.openapi.domain.dto.http.resonse;

import lombok.Data;

@Data
public class AgentChatDto {
    private String agentId = "0";
    private String userId = "0";
    private String lastChatTime = "0";
    private String updatedAt = "0";
    private Integer unreadCount = 0;
    private String name = "";
    private String description = "";
    private String avatarUrl;
    private String content = "";
    private String chatTime = "";
    private Integer role = 0;
}
