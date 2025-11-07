package com.openapi.domain.constant.realtime;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/11/7 13:47
 */
@Getter
public enum RealTimeChatStatue {
    /*
    会话状态：chatStatue
    1. 未会话 -> agent回复中(包含llm和tts) -> 会话结束
    2. 未会话 -> 录音中 -> 等待Agent回复 -> agent回复中(包含llm和tts) -> 会话结束
    3. 未会话 -> agent回复中 -> 等待function call结果 -> agent回复中 -> 会话结束
    4. 未会话 -> 录音中 -> 等待Agent回复 -> agent回复中 -> 等待function call结果 -> agent回复中 -> 会话结束
    状态管理需要用AtomicResource, 并且使用synchronized上锁，使用并发设计模式
    */
    // 未会话
    UNCONVERSATION("未会话"),
    // 录音中
    RECORDING("录音中"),
    // 等到Agent回复
    WAITING_AGENT_REPLY("等待Agent回复"),
    // agent回复中
    AGENT_REPLYING("agent回复中"),
    // 等待function call结果
    WAITING_FUNCTION_CALL_RESULT("等待function call结果"),
    // 会话结束
    CONVERSATION_END("会话结束"),
    ;
    private final String value;
    RealTimeChatStatue(String value) {
        this.value = value;
    }
    @NotNull
    public static RealTimeChatStatue getByValue(String value) {
        for (RealTimeChatStatue realTimeChatStatue : RealTimeChatStatue.values()) {
            if (realTimeChatStatue.value.equals(value)) {
                return realTimeChatStatue;
            }
        }
        return UNCONVERSATION;
    }
}
