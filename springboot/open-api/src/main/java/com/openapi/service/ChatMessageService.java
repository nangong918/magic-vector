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

    String insertOne(@NotNull ChatMessageDo chatMessageDo);

    List<ChatMessageDo> getMessagesByAgentIdDeadlineLimit(
            @NotNull String agentId,
            @NotNull LocalDateTime deadline,
            @NotNull Integer limit
    );

    @NotNull
    @Cacheable(value = "agentMessages", key = "#agentId")
    List<ChatMessageDo> getLast20Messages(@NotNull String agentId);

    @NotNull List<List<ChatMessageDo>> getLast20MessagesByAgentIds(@NotNull List<String> agentIds);
}
