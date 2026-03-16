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

    Long insertOne(@NotNull Long agentId, @NotNull String message, boolean isUser, Long userId);

    Long insertOne(@NotNull ChatMessageDo chatMessageDo);

    List<ChatMessageDo> getMessagesByAgentIdDeadlineLimit(
            @NotNull Long agentId,
            @NotNull LocalDateTime deadline,
            @NotNull Integer limit
    );

    List<ChatMessageDo> getMessagesBeforeAnchorLimit(
            @NotNull Long agentId,
            @NotNull Long anchorTimestamp,
            @NotNull Integer limit
    );

    List<ChatMessageDo> getMessagesAfterAnchorLimit(
            @NotNull Long agentId,
            @NotNull Long anchorTimestamp,
            @NotNull Integer limit
    );

    @NotNull
    @Cacheable(value = "agentMessages", key = "#agentId")
    List<ChatMessageDo> getLast10Messages(@NotNull Long agentId);

    @NotNull List<List<ChatMessageDo>> getLast10MessagesByAgentIds(@NotNull List<Long> agentIds);
}
