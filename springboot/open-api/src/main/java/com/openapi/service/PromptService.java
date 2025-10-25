package com.openapi.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author 13225
 * @date 2025/10/25 12:00
 * 提示词工程
 */
public interface PromptService {
    @NotNull String getSystemPrompt(@NotNull String systemPrompt);
    @Nullable Prompt getChatPromptWhitSystemPrompt(@NotNull String userMessage, @NotNull String systemPrompt);
}
