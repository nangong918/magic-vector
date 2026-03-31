package com.openapi.converter;

import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.ao.AgentChatAo;
import com.openapi.domain.dto.http.resonse.AgentChatDto;
import com.openapi.utils.DateUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class AgentHttpConverter {

    public AgentChatDto aoToDto(AgentAo agentAo) {
        return aoToDto(agentAo, null);
    }

    public AgentChatDto aoToDto(AgentAo agentAo, AgentChatAo agentChatAo) {
        AgentChatDto dto = new AgentChatDto();
        if (agentAo == null) {
            return dto;
        }

        dto.setAgentId(safeId(agentAo.getAgentId()));
        dto.setUserId(safeId(agentAo.getUserId()));
        dto.setUpdatedAt(safeId(agentAo.getUpdatedAt()));
        if (agentAo.getAgentVo() != null) {
            dto.setName(safe(agentAo.getAgentVo().getName()));
            dto.setDescription(safe(agentAo.getAgentVo().getDescription()));
            dto.setAvatarUrl(agentAo.getAgentVo().getAvatarUrl());
        }

        if (agentChatAo == null) {
            return dto;
        }

        dto.setLastChatTime(String.valueOf(agentChatAo.getLastChatTime() == null ? 0L : agentChatAo.getLastChatTime()));
        dto.setUnreadCount(agentChatAo.getUnreadCount() == null ? 0 : agentChatAo.getUnreadCount());

        if (agentChatAo.getLastChatMessages() != null && !agentChatAo.getLastChatMessages().isEmpty()) {
            ChatMessageDo latest = agentChatAo.getLastChatMessages().stream()
                    .max(Comparator.comparing(ChatMessageDo::getChatTimestamp, Comparator.nullsLast(Long::compareTo)))
                    .orElse(null);
            dto.setContent(safe(latest.getContent()));
            dto.setRole(latest.getRole() == null ? 0 : latest.getRole());
            if (latest.getChatTime() != null) {
                dto.setChatTime(DateUtils.yyyyMMddHHmmssToString(latest.getChatTime()));
            }
        }
        return dto;
    }

    public AgentChatDto chatAoToDto(AgentChatAo agentChatAo) {
        if (agentChatAo == null) {
            return new AgentChatDto();
        }
        return aoToDto(agentChatAo.getAgentAo(), agentChatAo);
    }

    private String safeId(String value) {
        return value == null ? "0" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
