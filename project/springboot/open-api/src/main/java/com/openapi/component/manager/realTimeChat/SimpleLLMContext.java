package com.openapi.component.manager.realTimeChat;

import com.openapi.domain.entity.LLMContext;
import com.openapi.domain.entity.TTSContext;
import lombok.Getter;
import lombok.NonNull;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 13225
 * @date 2025/11/7 11:48
 */
public class SimpleLLMContext {
    /// LLM
    // llm重试次数（function call LLM 和 result LLM 共享）
    @Getter
    private final LLMContext llmContext = new LLMContext();
    /// TTS
    // tts context
    @Getter
    private final TTSContext ttsContext = new TTSContext();
    // tts MQ
    private final Queue<String> ttsMQ = new ConcurrentLinkedQueue<>();


    public void offerTTS(String tts) {
        ttsMQ.offer(tts);
    }

    @NonNull
    public String getAllTTS() {
        StringBuilder sb = new StringBuilder();
        while (!ttsMQ.isEmpty()) {
            sb.append(ttsMQ.poll());
        }
        return sb.toString();
    }

    public void reset() {
        llmContext.reset();
        ttsContext.reset();
        ttsMQ.clear();
    }

    public int getAllTTSCount() {
        return ttsMQ.size();
    }
}
