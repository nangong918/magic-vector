package com.openapi.domain.constant.ws;

import lombok.Getter;
import lombok.NonNull;

public enum WsChannel {

    /**
     * none
     */
    NONE("none"),

    /**
     * 连接管理、心跳保活
     */
    CONNECTION("connection"),

    /**
     * Agent配置同步
     */
    AGENT("agent"),

    /**
     * 语音识别数据流
     */
    STT("stt"),

    /**
     * LLM文本流
     */
    LLM("llm"),

    /**
     * TTS音频流
     */
    TTS("tts"),

    /**
     * 视觉理解数据流（V2版本由UDP或RTMP替代）
     */
    VL("vl"),

    /**
     * 设备控制命令
     */
    CONTROL("control"),

    /**
     * 指令批量编排（TTS + MCP 有序列表，与 {@link WsEvent#INSTRUCTION_RESULT} 成对）
     */
    INSTRUCTION_LIST("instruction_list"),

    /**
     * 设备状态上报与查询
     */
    STATUS("status"),

    /**
     * 系统消息与错误通知
     */
    SYSTEM("system");

    @Getter
    private final String value;

    WsChannel(String value) {
        this.value = value;
    }

    /**
     * 根据字符串值获取枚举，若未找到则返回 {@link #NONE}。
     * @param value 字符串值，不能为 null
     * @return 对应的枚举，若未匹配则返回 NONE
     */
    @NonNull
    public static WsChannel fromValue(@NonNull String value) {
        for (WsChannel channel : values()) {
            if (channel.value.equals(value)) {
                return channel;
            }
        }
        return NONE;
    }
}