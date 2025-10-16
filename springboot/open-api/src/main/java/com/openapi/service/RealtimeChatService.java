package com.openapi.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.openapi.component.manager.RealtimeChatContextManager;
import com.openapi.domain.exception.AppException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @author 13225
 * @date 2025/10/16 10:08
 */
public interface RealtimeChatService {
    void startChat(@NotNull RealtimeChatContextManager chatContextManager, ChatClient chatClient) throws InterruptedException, NoApiKeyException;

    ChatClient initChatClient(@NotNull RealtimeChatContextManager chatContextManager, @NotNull DashScopeChatModel chatModel) throws AppException;
}
