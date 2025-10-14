package com.openapi.service;

import com.openapi.domain.Do.ChatMessageDo;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 13225
 * @date 2025/9/30 16:09
 */
public interface ChatMessageService {

    String insertOne(@NotNull String agentId, @NotNull String message, boolean isUser, String userId);

    List<ChatMessageDo> getMessagesByAgentIdDeadlineLimit(
            @NotNull String agentId,
            @NotNull LocalDateTime deadline,
            @NotNull Integer limit
    );

    @Cacheable
    List<ChatMessageDo> getLast20Messages(@NotNull String agentId);
}
