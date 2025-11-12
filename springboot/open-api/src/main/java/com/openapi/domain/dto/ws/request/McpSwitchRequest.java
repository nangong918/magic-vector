package com.openapi.domain.dto.ws.request;

import com.openapi.domain.ao.mixLLM.McpSwitch;
import com.openapi.domain.constant.realtime.RealtimeSystemRequestEventEnum;
import lombok.Data;

/**
 * @author 13225
 * @date 2025/11/11 16:03
 */
@Data
public class McpSwitchRequest {
    public String event = RealtimeSystemRequestEventEnum.SUBMIT_MCP_SWITCH.getCode();
    public String agentId;
    public String userId;
    public String messageId;
    public McpSwitch mcpSwitch;
}
