package com.openapi.service.test;

import com.alibaba.dashscope.audio.omni.OmniRealtimeConversation;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/10/7 17:39
 */
public interface OmniRealTimeNoVADTestService {

    OmniRealtimeConversation getOmniRealtimeConversation(
            @NotNull WebSocketSession session);

    void setOmniRealtimeConversationConfig(@NotNull OmniRealtimeConversation conversation) throws NoApiKeyException, InterruptedException;

    void startChat(@NotNull OmniRealtimeConversation conversation,
                   @NotNull AtomicBoolean stopRecording,
                   @NotNull Queue<byte[]> requestAudioBuffer) throws IOException, InterruptedException;
}
