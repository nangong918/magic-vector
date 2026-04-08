package com.openapi.domain.constant.realtime;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/11/7 13:47
 */
@Getter
public enum RealTimeChatState {
    /*
    会话状态：chatState
    1. 未会话 -> agent回复中(包含llm和tts) -> 会话结束
    2. 未会话 -> 录音中 -> 等待Agent回复 -> agent回复中(包含llm和tts) -> 会话结束
    状态管理需要用AtomicResource, 并且使用synchronized上锁，使用并发设计模式
    */
    // 未会话
    UNCONVERSATION(0, "未会话"),
    // 录音中
    RECORDING(1, "录音中"),
    // 等到Agent回复 (莫信号调用中)
    WAITING_AGENT_REPLY(2, "等待Agent回复"),
    // agent回复中
    AGENT_REPLYING(3, "agent回复中"),
    // 会话结束
    CONVERSATION_END(4, "会话结束"),
    ;
    private final int code;
    private final String value;
    RealTimeChatState(int code, String value) {
        this.code = code;
        this.value = value;
    }

    // code -> enum
    @NotNull
    public static RealTimeChatState getByCode(int code) {
        for (RealTimeChatState value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNCONVERSATION;
    }
}
