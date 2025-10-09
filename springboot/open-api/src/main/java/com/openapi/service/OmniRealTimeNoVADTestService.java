package com.openapi.service;

import com.alibaba.dashscope.exception.NoApiKeyException;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/10/7 17:39
 */
public interface OmniRealTimeNoVADTestService {
    void audioChat(
            Queue<String> b64AudioBuffer,
            Queue<byte[]> rawAudioBuffer,
            AtomicBoolean stopConversation,
            AtomicBoolean stopRecording
    ) throws NoApiKeyException, InterruptedException, IOException;
}
