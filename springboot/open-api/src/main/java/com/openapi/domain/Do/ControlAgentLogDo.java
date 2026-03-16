package com.openapi.domain.Do;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ControlAgentLogDo {
    @Id
    private Long id;
    private Long userId;
    private Long agentId;
    private Long logTime;
    private String logContent;
}
