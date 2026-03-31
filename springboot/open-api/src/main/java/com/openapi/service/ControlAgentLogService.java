package com.openapi.service;

import com.openapi.domain.dto.http.resonse.ControlAgentLogResponse;

public interface ControlAgentLogService {
    void saveControlLog(Long userId, Long agentId, String logContent, Long logTime);

    ControlAgentLogResponse queryLogs(Long userId, Long agentId, Integer page, Integer size);
}
