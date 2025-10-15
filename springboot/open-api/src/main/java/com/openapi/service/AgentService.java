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
    AgentAo createAgent(@Nullable MultipartFile avatar, @NotNull String userId, @NotNull String name, @NotNull String description);

    AgentAo getAgentById(String id);

    @NotNull List<AgentAo> getAgentsByIds(List<String> ids);

    @NotNull List<String> getUserAgents(String userId);

    @NotNull List<AgentAo> getUserAgentsAo(String userId);

    @NotNull List<AgentChatAo> getLastAgentChatList(@NotNull String userId);
}
