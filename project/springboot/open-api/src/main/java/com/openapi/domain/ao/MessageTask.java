package com.openapi.domain.ao;

import lombok.Data;

/**
 * @author 13225
 * @date 2025/11/4 15:35
 */
@Data
public class MessageTask {
    public String agentId;
    public String message;

    public MessageTask(String agentId, String message) {
        this.agentId = agentId;
        this.message = message;
    }
}
