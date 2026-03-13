package com.openapi.domain.Do;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ControlAgentLogDo {
    @Id
    private Long id;
    private String userId;
    private String agentId;
    private Long logTime;
    private String logContent;
}
