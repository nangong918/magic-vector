package com.openapi.domain.constant.ws;


import lombok.Getter;
import lombok.NonNull;

public enum WsChannel {
    // None 值
    NONE("none"),

    // 连接管理
    CONNECT("connect"),
    CONNECT_ACK("connect_ack"),
    RK_CONNECT("rk_connect"),
    RK_CONNECT_ACK("rk_connect_ack"),
    PING("ping"),
    PONG("pong"),

    // Agent数据同步
    AGENT_LIST_SYNC("agent_list_sync"),
    AGENT_UPDATE("agent_update"),

    // 聊天消息
    CHAT_MESSAGE_SYNC("chat_message_sync"),
    CHAT_MESSAGE_SEND("chat_message_send"),

    // STT 语音识别
    STT_START("stt_start"),
    STT_START_ACK("stt_start_ack"),
    STT_AUDIO_DATA("stt_audio_data"),
    STT_TEXT_DATA("stt_text_data"),
    STT_END("stt_end"),
    STT_ERROR("stt_error"),

    // LLM 语言模型
    LLM_START("llm_start"),
    LLM_DATA("llm_data"),
    LLM_END("llm_end"),
    LLM_ERROR("llm_error"),

    // TTS 语音合成
    TTS_START("tts_start"),
    TTS_DATA("tts_data"),
    TTS_END("tts_end"),
    TTS_ERROR("tts_error"),

    // VL 视觉理解
    VL_START("vl_start"),
    VL_DATA("vl_data"),
    VL_END("vl_end"),
    VL_ERROR("vl_error"),

    // 控制命令
    CONTROL_COMMAND_AN("control_command_an"),
    CONTROL_COMMAND_SB("control_command_sb"),
    COMMAND_RESULT("command_result"),
    CONTROL_RESPONSE("control_response"),

    // 设备状态
    STATUS_REQUEST("status_request"),
    RK_STATUS("rk_status"),

    // 系统消息
    ERROR("error"),
    SYSTEM_MESSAGE("system_message");

    @Getter
    private final String value;

    WsChannel(String value) {
        this.value = value;
    }

    /**
     * 根据字符串值获取枚举
     */
    @NonNull
    public static WsChannel fromValue(String value) {
        for (WsChannel channel : values()) {
            if (channel.value.equals(value)) {
                return channel;
            }
        }
        return NONE;
    }
}
