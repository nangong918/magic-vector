package com.openapi.domain.dto.resonse;

import com.openapi.domain.vo.AgentVo;
import lombok.Data;

/**
 * @author 13225
 * @date 2025/9/29 13:37
 */
@Data
public class CreateAgentResponse {
    // 创建的Agent id (也是session id)
    public String agentId;
    // 创建的Agent vo
    public AgentVo agentVo;
}
