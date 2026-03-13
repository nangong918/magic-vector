package com.data.domain.dto.response;

import java.util.ArrayList;
import java.util.List;

public class ControlAgentLogResponse {
    public List<ControlAgentLogItem> logs = new ArrayList<>();

    public static class ControlAgentLogItem {
        public Long id;
        public Long userId;
        public Long agentId;
        public Long logTime;
        public String logContent;
    }
}
