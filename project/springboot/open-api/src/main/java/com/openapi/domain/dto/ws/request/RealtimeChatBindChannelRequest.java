package com.openapi.domain.dto.ws.request;

import lombok.Data;

@Data
public class RealtimeChatBindChannelRequest {
    // agent channel id
    public String agentId;
    // bind timestamp
    public long timestamp;
}
