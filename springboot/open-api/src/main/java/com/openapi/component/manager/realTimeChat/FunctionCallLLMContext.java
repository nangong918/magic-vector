package com.openapi.component.manager.realTimeChat;

import com.openapi.domain.entity.LLMContext;
import com.openapi.domain.entity.TTSContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class FunctionCallLLMContext {
    // 剩余等待的function call信号量 （封装Function Call）
    private final AtomicInteger remainingFunctionCallSignal = new AtomicInteger(0);
    // ResultList
    private final List<String> functionCallResultList = new LinkedList<>();
    // 所有function call完成回调
    public AllFunctionCallFinished allFunctionCallFinished;
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
    public synchronized void addFunctionCallSignal() {
        remainingFunctionCallSignal.incrementAndGet();
    }
    private synchronized int removeFunctionCallSignal() {
        return remainingFunctionCallSignal.decrementAndGet();
    }

    public synchronized int getRemainingFunctionCallSignal() {
        return remainingFunctionCallSignal.get();
    }

    public synchronized void addFunctionCallResult(String result) {
        functionCallResultList.add(result);
        // 添加一条响应结果，减少一条信号量
        var remainingFunctionCallSignal = removeFunctionCallSignal();
        // 检查是否可以执行最后result的LLM；检查点1: function call获取到result -> 对应tts比function call快
        if (remainingFunctionCallSignal > 0) {
            log.info("[FunctionCall] 添加FunctionCall结果，剩余function call信号量：{}", remainingFunctionCallSignal);
        }
        else {
            if (allFunctionCallFinished != null){
                log.info("[FunctionCall] 检查点1 所有FunctionCall结果获取完毕，开始执行最后result的LLM");
                allFunctionCallFinished.allFunctionCallFinished();
            }
        }
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

    public int getAllTTSCount() {
        return ttsMQ.size();
    }
}
