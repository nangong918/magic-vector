package com.openapi.service;

import com.alibaba.dashscope.exception.NoApiKeyException;
import org.jetbrains.annotations.NotNull;
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

    void startTextChat(@NotNull String userQuestion, @NotNull WebSocketSession session) throws IOException;
}
