package com.openapi.domain.entity;

import com.openapi.domain.entity.realtimeChat.STTContext;
import com.openapi.domain.entity.realtimeChat.LLMContext;
import com.openapi.domain.entity.realtimeChat.TTSContext;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 13225
 * @date 2025/11/7 11:48
 */
public class SimpleLLMContext {
    /// 音频
    // 音频状态
    @Getter
    private final STTContext sstRecordContext = new STTContext();
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

    public String getAllTTS() {
        StringBuilder sb = new StringBuilder();
        while (!ttsMQ.isEmpty()) {
            sb.append(ttsMQ.poll());
        }
        return sb.toString();
    }

    public void reset() {
        sstRecordContext.reset();
        llmContext.reset();
        ttsContext.reset();
        ttsMQ.clear();
    }
}
