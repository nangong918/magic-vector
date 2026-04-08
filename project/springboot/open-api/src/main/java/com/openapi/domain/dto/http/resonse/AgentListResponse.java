package com.openapi.domain.dto.http.resonse;

import lombok.Data;

import java.util.List;

/**
 * @author 13225
 * @date 2025/9/29 13:37
 */
@Data
public class AgentListResponse {
    private List<AgentChatDto> agentList;
}
