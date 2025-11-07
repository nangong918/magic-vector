package com.openapi.domain.entity;


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 13225
 * @date 2025/11/7 13:06
 */
public class LLMContext {
    // llm重试次数（function call LLM 和 result LLM 共享）
    private final AtomicInteger llmConnectResetRetryCount = new AtomicInteger(0);
    // 是否正在llm
    private final AtomicBoolean isLLMing = new AtomicBoolean(false);

    // 获取当前重试次数
    public int getLlmConnectResetRetryCount() {
        // 增加并返回
        return llmConnectResetRetryCount.incrementAndGet();
    }
    public AtomicInteger getLlmConnectResetRetryCountAtomic() {
        return llmConnectResetRetryCount;
    }

    // 是否正在llm
    public boolean isLLMing() {
        return isLLMing.get();
    }

    // 设置是否正在llm
    public void setLLMing(boolean llMing) {
        isLLMing.set(llMing);
    }

    // 重置
    public void reset() {
        // 重置重试次数
        llmConnectResetRetryCount.set(0);
        // 重置是否正在llm
        isLLMing.set(false);
    }
}
