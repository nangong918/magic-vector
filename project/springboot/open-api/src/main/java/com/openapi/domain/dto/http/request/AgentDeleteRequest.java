package com.openapi.domain.dto.http.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentDeleteRequest {
    @NotBlank(message = "agentId不能为空")
    private String agentId;

    @NotBlank(message = "userId不能为空")
    private String userId;
}
