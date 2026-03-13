package com.openapi.service;

import com.openapi.domain.dto.resonse.ControlAgentLogResponse;

public interface ControlAgentLogService {
    void saveControlLog(String userId, String agentId, String logContent, Long logTime);

    ControlAgentLogResponse queryLogs(String userId, String agentId, Integer page, Integer size);
}
