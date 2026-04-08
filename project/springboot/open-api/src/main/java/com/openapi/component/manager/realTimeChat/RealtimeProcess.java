package com.openapi.component.manager.realTimeChat;

/**
 * @author 13225
 * @date 2025/11/7 17:41
 */
public interface RealtimeProcess {
    void startRecord();
    void stopRecord();

    void startLLM();
    void stopLLM();

    void startTTS();
    void stopTTS();

    void endConversation();

    void reset();
}
