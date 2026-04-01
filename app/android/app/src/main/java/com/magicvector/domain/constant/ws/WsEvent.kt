package com.magicvector.domain.constant.ws

enum class WsEvent(val value: String) {
    // None 值
    NONE("none"),

    // 连接管理
    /**
     * @see com.magicvector.domain.dto.ws.request.ConnectRequest
     */
    CONNECT("connect"),
    /**
     * @see com.magicvector.domain.dto.ws.response.ConnectResponse
     */
    CONNECT_ACK("connect_ack"),
    /**
     * @see com.magicvector.domain.dto.ws.request.RkConnectRequest
     */
    RK_CONNECT("rk_connect"),
    /**
     * @see com.magicvector.domain.dto.ws.response.RkConnectResponse
     */
    RK_CONNECT_ACK("rk_connect_ack"),
    PING("ping"),
    PONG("pong"),

    // Agent数据同步
    /**
     * @see com.magicvector.domain.dto.http.response.AgentListResponse
     */
    AGENT_LIST_SYNC("agent_list_sync"),
    /**
     * @see com.magicvector.domain.dto.http.response.AgentChatDto
     */
    AGENT_UPDATE("agent_update"),

    // 聊天消息
    /**
     * @see com.magicvector.domain.dto.http.response.ChatMessageListResponse
     */
    CHAT_MESSAGE_SYNC("chat_message_sync"),
    /**
     * @see com.magicvector.domain.dto.http.response.ChatMessageDto
     */
    CHAT_MESSAGE_SEND("chat_message_send"),

    // STT 语音识别
    /**
     * @see com.magicvector.domain.dto.ws.request.SttStartRequest
     */
    STT_START("stt_start"),
    /**
     * @see com.magicvector.domain.dto.ws.response.SttStartResponse
     */
    STT_START_ACK("stt_start_ack"),
    /**
     * @see com.magicvector.domain.dto.ws.request.SttAudioDataRequest
     */
    STT_AUDIO_DATA("stt_audio_data"),
    /**
     * @see com.magicvector.domain.dto.ws.response.SttTextDataResponse
     */
    STT_TEXT_DATA("stt_text_data"),
    /**
     * @see com.magicvector.domain.dto.ws.request.SttEndRequest
     */
    STT_END("stt_end"),
    /**
     * @see com.magicvector.domain.dto.ws.response.SttErrorResponse
     */
    STT_ERROR("stt_error"),

    // LLM 语言模型
    /**
     * @see com.magicvector.domain.dto.ws.response.LlmStartResponse
     */
    LLM_START("llm_start"),
    /**
     * @see com.magicvector.domain.dto.ws.response.LlmDataResponse
     */
    LLM_DATA("llm_data"),
    /**
     * @see com.magicvector.domain.dto.ws.response.LlmEndResponse
     */
    LLM_END("llm_end"),
    /**
     * @see com.magicvector.domain.dto.ws.response.LlmErrorResponse
     */
    LLM_ERROR("llm_error"),

    // TTS 语音合成
    /**
     * @see com.magicvector.domain.dto.ws.response.TtsStartResponse
     */
    TTS_START("tts_start"),
    /**
     * @see com.magicvector.domain.dto.ws.response.TtsDataResponse
     */
    TTS_DATA("tts_data"),
    /**
     * @see com.magicvector.domain.dto.ws.response.TtsEndResponse
     */
    TTS_END("tts_end"),
    /**
     * @see com.magicvector.domain.dto.ws.response.TtsErrorResponse
     */
    TTS_ERROR("tts_error"),

    // VL 视觉理解
    /**
     * @see com.magicvector.domain.dto.ws.request.VlStartRequest
     */
    VL_START("vl_start"),
    /**
     * @see com.magicvector.domain.dto.ws.response.VlDataResponse
     */
    VL_DATA("vl_data"),
    /**
     * @see com.magicvector.domain.dto.ws.response.VlEndResponse
     */
    VL_END("vl_end"),
    /**
     * @see com.magicvector.domain.dto.ws.response.VlErrorResponse
     */
    VL_ERROR("vl_error"),

    // 控制命令
    /**
     * @see com.magicvector.domain.dto.ws.request.ControlCommandRequest
     */
    CONTROL_COMMAND_AN("control_command_an"),
    /**
     * @see com.magicvector.domain.dto.ws.response.ControlCommandResponse
     */
    CONTROL_COMMAND_SB("control_command_sb"),
    /**
     * @see com.magicvector.domain.dto.ws.request.CommandResultRequest
     */
    COMMAND_RESULT("command_result"),
    /**
     * @see com.magicvector.domain.dto.ws.response.ControlResponse
     */
    CONTROL_RESPONSE("control_response"),

    // 设备状态
    /**
     * @see com.magicvector.domain.dto.ws.request.StatusRequest
     */
    STATUS_REQUEST("status_request"),
    /**
     * @see com.magicvector.domain.dto.ws.response.RkStatusResponse
     */
    RK_STATUS("rk_status"),

    // 系统消息
    /**
     * @see com.magicvector.domain.dto.ws.response.ErrorResponse
     */
    ERROR("error"),
    /**
     * @see com.magicvector.domain.dto.ws.response.SystemMessageResponse
     */
    SYSTEM_MESSAGE("system_message");


    companion object {
        /**
         * 根据字符串值获取枚举
         */
        fun fromValue(value: String): WsEvent {
            return entries.find { it.value == value } ?: NONE
        }
    }

}