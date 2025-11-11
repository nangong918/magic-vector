package com.data.domain.dto.ws.request;


import com.data.domain.ao.mixLLM.McpSwitch;
import com.data.domain.constant.chat.RealtimeSystemRequestEventEnum;

/**
 * @author 13225
 * @date 2025/11/11 16:03
 */
public class McpSwitchRequest {
    public String event = RealtimeSystemRequestEventEnum.SUBMIT_MCP_SWITCH.getCode();
    public String agentId;
    public String userId;
    public String messageId;
    public McpSwitch mcpSwitch;
}
