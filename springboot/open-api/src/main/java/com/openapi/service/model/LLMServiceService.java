package com.openapi.service.model;

import com.openapi.domain.ao.realtimeChat.McpSwitch;
import com.openapi.interfaces.model.LLMErrorCallback;
import com.openapi.interfaces.model.LLMStateCallback;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @author 13225
 * @date 2025/11/8 14:21
 */
public interface LLMServiceService {
    reactor.core.Disposable LLMStreamCall(
            @NonNull String sentence,
            @NonNull ChatClient chatClient,
            @NonNull String agentId,
            @Nullable String currentContextParam,
            @NonNull LLMStateCallback callback,
            @NonNull Object... functionCallTools);

    String mixLLMCall(
            @NonNull String sentence,
            @NonNull ChatClient chatClient,
            @NonNull String agentId,
            @NonNull String currentContextParam,
            @NonNull McpSwitch mcpSwitch,
            @Nullable Object... functionCallTools
    );

    String mixLLMCallErrorProxy(
            @NonNull String sentence,
            @NonNull ChatClient chatClient,
            @NonNull String agentId,
            @NonNull String currentContextParam,
            @NonNull McpSwitch mcpSwitch,
            @NonNull LLMErrorCallback errorCallback,
            @Nullable Object... functionCallTools
    );

    reactor.core.Disposable functionCallLLMStreamChat(
            @Nullable String result,
            @NonNull String userQuestion,
            @NonNull ChatClient chatClient,
            @NonNull String agentId,
            @NonNull LLMStateCallback callback
    );
}
