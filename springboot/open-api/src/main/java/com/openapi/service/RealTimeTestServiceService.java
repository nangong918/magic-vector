package com.openapi.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/10/13 16:59
 */
public interface RealTimeTestServiceService {
    void startChat(@NotNull AtomicBoolean stopRecording,
                   @NotNull Queue<byte[]> requestAudioBuffer,
                   @NotNull WebSocketSession session) throws IOException, InterruptedException, NoApiKeyException;

    @NotNull ChatClient initChatClient(@NotNull DashScopeChatModel chatModel);

    void startTextChat(@NotNull String userQuestion, @NotNull WebSocketSession session, @NotNull ChatClient chatClient) throws IOException;
}
