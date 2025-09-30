package com.openapi.service;

import com.openapi.domain.ao.AgentAo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 13225
 * @date 2025/9/29 16:26
 */
public interface AgentService {
    AgentAo createAgent(@Nullable MultipartFile avatar, @NotNull String name, @NotNull String description);

    AgentAo getAgentById(String id);
}
