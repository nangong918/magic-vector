package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String id = "0";
    private String agentId = "0";
    private String userId = "0";
    private String messageId = "0";
    private String timestamp = "0";
    private String imgUrl = "";
    private Integer messageType = 0;
    private String content = "";
    private String chatTime = "";
    private Integer role = 0;
}
