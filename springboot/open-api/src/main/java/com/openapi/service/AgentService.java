package com.openapi.service;

import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.ao.AgentChatAo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 13225
 * @date 2025/9/29 16:26
 */
public interface AgentService {
    AgentAo createAgent(@Nullable MultipartFile avatar, @NotNull Long userId, @NotNull String name, @NotNull String description);

    AgentAo updateAgent(
            @Nullable MultipartFile avatar,
            @NotNull Long agentId,
            @NotNull Long userId,
            @NotNull String name,
            @NotNull String description
    );

    boolean deleteAgent(@NotNull Long agentId, @NotNull Long userId);

    AgentAo getAgentById(Long id);

    @NotNull List<AgentAo> getAgentsByIds(List<Long> ids);

    @NotNull List<Long> getUserAgents(Long userId);

    @NotNull List<AgentAo> getUserAgentsAo(Long userId);

    @NotNull List<AgentChatAo> getLastAgentChatList(@NotNull Long userId);
}
