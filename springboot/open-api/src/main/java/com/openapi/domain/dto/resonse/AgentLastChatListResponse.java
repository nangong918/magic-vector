package com.openapi.domain.dto.resonse;

import com.openapi.domain.ao.AgentChatAo;
import lombok.Data;

import java.util.List;

/**
 * @author 13225
 * @date 2025/10/15 11:31
 */
@Data
public class AgentLastChatListResponse {
    private List<AgentChatAo> agentChatAos;
}
