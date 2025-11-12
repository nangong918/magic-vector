package com.openapi.interfaces.mixLLM;

/**
 * @author 13225
 * @date 2025/11/11 14:39
 */
public interface LLMCallback {
    // 处理固定String
    void handleResult(String result);
    // 处理流式String
}
