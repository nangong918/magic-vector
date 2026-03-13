package com.openapi.domain.dto.resonse;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ControlAgentLogResponse {
    private List<ControlAgentLogItem> logs = new ArrayList<>();

    @Data
    public static class ControlAgentLogItem {
        private Long id;
        private Long userId;
        private Long agentId;
        private Long logTime;
        private String logContent;
    }
}
