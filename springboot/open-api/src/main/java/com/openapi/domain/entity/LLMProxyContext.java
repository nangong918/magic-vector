package com.openapi.domain.entity;

import com.openapi.domain.constant.realtime.RealTimeChatStatue;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 13225
 * @date 2025/11/7 11:51
 * 设计模式：代理模式
 * 管理：普通对话模式和functionCall模式
 * @see SimpleLLMContext
 * @see FunctionCallLLMContext
 */
public class LLMProxyContext {
    // 会话状态
    private final RealTimeChatStatue realTimeChatStatue = RealTimeChatStatue.UNCONVERSATION;
    //  会话Context
    private final SimpleLLMContext simpleLLMContext = new SimpleLLMContext();
    private final FunctionCallLLMContext functionCallLLMContext = new FunctionCallLLMContext();
    /// 是否是音频对话
    private final AtomicBoolean isAudioChat = new AtomicBoolean(false);
    // 是否是FunctionCall会话
    private final AtomicBoolean isFunctionCall = new AtomicBoolean(false);

    public void setIsAudioChat(boolean isAudioChat) {
        this.isAudioChat.set(isAudioChat);
    }
    public boolean isAudioChat() {
        return isAudioChat.get();
    }
    public void setIsFunctionCall(boolean isFunctionCall) {
        this.isFunctionCall.set(isFunctionCall);
    }
    public boolean isFunctionCall() {
        return isFunctionCall.get();
    }
}
