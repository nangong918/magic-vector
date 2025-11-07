package com.openapi.component.manager.realTimeChat;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/11/7 18:47
 */
public interface ChatRealtimeStatue {
    // LLM
    int getLLMErrorCount();
    boolean isLLMing();

    // STT
    boolean isAudioChat();
    boolean isRecording();

    // TTS
    AtomicBoolean isFirstTTS();
    boolean isTTSing();
    long getTTSStartTime();
    void setTTSStartTime(long time);
}
