package com.openapi.domain.entity;

import com.openapi.domain.entity.realtimeChat.STTContext;
import com.openapi.domain.entity.realtimeChat.LLMContext;
import com.openapi.domain.entity.realtimeChat.TTSContext;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 13225
 * @date 2025/11/7 11:48
 */
public class FunctionCallLLMContext {
    // 剩余等待的function call信号量 （封装Function Call）
    private final AtomicInteger remainingFunctionCallSignal = new AtomicInteger(0);
    // ResultList
    private final List<String> functionCallResultList = new LinkedList<>();
    /// LLM
    // llm重试次数（function call LLM 和 result LLM 共享）
    @Getter
    private final LLMContext llmContext = new LLMContext();
    /// TTS
    // tts context
    @Getter
    private final TTSContext ttsContext = new TTSContext();
    // 是否是最后result的tts
    private final AtomicBoolean isFinalResultTTS = new AtomicBoolean(false);
    // tts MQ
    private final Queue<String> ttsMQ = new ConcurrentLinkedQueue<>();

    // 添加function call信号量
    public void addFunctionCallSignal() {
        remainingFunctionCallSignal.incrementAndGet();
    }

    public int getRemainingFunctionCallSignal() {
        return remainingFunctionCallSignal.get();
    }

    public void addFunctionCallResult(String result) {
        functionCallResultList.add(result);
    }
    
    public String getAllFunctionCallResult() {
        StringBuilder sb = new StringBuilder();
        for (String result : functionCallResultList) {
            sb.append(result);
        }
        return sb.toString();
    }

    public void setIsFinalResultTTS(boolean isFinalResultTTS){
        this.isFinalResultTTS.set(isFinalResultTTS);
    }

    public boolean isFinalResultTTS() {
        return isFinalResultTTS.get();
    }

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
        remainingFunctionCallSignal.set(0);
        functionCallResultList.clear();
        llmContext.reset();
        ttsContext.reset();
        isFinalResultTTS.set(false);
        ttsMQ.clear();
    }
}
