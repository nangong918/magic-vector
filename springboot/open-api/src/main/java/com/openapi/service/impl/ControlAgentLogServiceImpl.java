package com.openapi.service.impl;

import cn.hutool.core.util.IdUtil;
import com.openapi.domain.Do.ControlAgentLogDo;
import com.openapi.domain.dto.resonse.ControlAgentLogResponse;
import com.openapi.mapper.ControlAgentLogMapper;
import com.openapi.service.ControlAgentLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlAgentLogServiceImpl implements ControlAgentLogService {
    private final ControlAgentLogMapper controlAgentLogMapper;

    @Override
    public void saveControlLog(String userId, String agentId, String logContent, Long logTime) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(agentId) || !StringUtils.hasText(logContent)) {
            return;
        }
        ControlAgentLogDo logDo = new ControlAgentLogDo();
        logDo.setId(IdUtil.getSnowflakeNextId());
        logDo.setUserId(userId);
        logDo.setAgentId(agentId);
        logDo.setLogTime(logTime == null ? System.currentTimeMillis() : logTime);
        logDo.setLogContent(logContent);
        controlAgentLogMapper.insert(logDo);
    }

    @Override
    public ControlAgentLogResponse queryLogs(String userId, String agentId, Integer page, Integer size) {
        int fixedPage = page == null || page <= 0 ? 1 : page;
        int fixedSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
        int offset = (fixedPage - 1) * fixedSize;
        List<ControlAgentLogDo> list = controlAgentLogMapper.queryByUserAgentPage(
                userId,
                agentId,
                offset,
                fixedSize
        );
        ControlAgentLogResponse response = new ControlAgentLogResponse();
        if (list == null) {
            return response;
        }
        list.forEach(it -> {
            ControlAgentLogResponse.ControlAgentLogItem item = new ControlAgentLogResponse.ControlAgentLogItem();
            item.setId(it.getId());
            item.setUserId(parseLongOrZero(it.getUserId()));
            item.setAgentId(parseLongOrZero(it.getAgentId()));
            item.setLogTime(it.getLogTime());
            item.setLogContent(it.getLogContent());
            response.getLogs().add(item);
        });
        return response;
    }

    private Long parseLongOrZero(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignore) {
            return 0L;
        }
    }
}
