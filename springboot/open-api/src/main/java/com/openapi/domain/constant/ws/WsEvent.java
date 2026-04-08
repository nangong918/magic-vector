package com.openapi.domain.constant.ws;


import lombok.Getter;
import lombok.NonNull;

public enum WsEvent {
    /// None 值
    NONE("none", WsChannel.SYSTEM),

    /// 连接管理
    /**
     * @see com.openapi.domain.dto.ws.request.ConnectRequest
     */
    CONNECT("connect", WsChannel.CONNECTION),
    /**
     * @see com.openapi.domain.dto.ws.response.ConnectResponse
     */
    CONNECT_ACK("connect_ack", WsChannel.CONNECTION),
    /**
     * @see com.openapi.domain.dto.ws.request.RkConnectRequest
     */
    RK_CONNECT("rk_connect", WsChannel.CONNECTION),
    /**
     * @see com.openapi.domain.dto.ws.response.RkConnectResponse
     */
    RK_CONNECT_ACK("rk_connect_ack", WsChannel.CONNECTION),
    PING("ping", WsChannel.CONNECTION),
    PONG("pong", WsChannel.CONNECTION),

    /// Agent数据同步
    /**
     * @see com.openapi.domain.dto.http.resonse.AgentListResponse
     */
    AGENT_LIST_SYNC("agent_list_sync", WsChannel.AGENT),
    /**
     * @see com.openapi.domain.dto.http.resonse.AgentChatDto
     */
    AGENT_UPDATE("agent_update", WsChannel.AGENT),

    /// STT 语音识别
    /**
     * @see com.openapi.domain.dto.ws.request.SttStartRequest
     */
    STT_START("stt_start", WsChannel.STT),
    /**
     * @see com.openapi.domain.dto.ws.response.SttStartResponse
     */
    STT_START_ACK("stt_start_ack", WsChannel.STT),
    /**
     * @see com.openapi.domain.dto.ws.request.SttAudioDataRequest
     */
    STT_AUDIO_DATA("stt_audio_data", WsChannel.STT),
    /**
     * @see com.openapi.domain.dto.ws.response.SttTextDataResponse
     */
    STT_TEXT_DATA("stt_text_data", WsChannel.STT),
    /**
     * @see com.openapi.domain.dto.ws.request.SttEndRequest
     */
    STT_END("stt_end", WsChannel.STT),
    /**
     * @see com.openapi.domain.dto.ws.response.SttErrorResponse
     */
    STT_ERROR("stt_error", WsChannel.STT),

    /// LLM 语言模型
    /**
     * @see com.openapi.domain.dto.ws.response.LlmStartResponse
     */
    LLM_START("llm_start", WsChannel.LLM),
    /**
     * @see com.openapi.domain.dto.ws.response.LlmDataResponse
     */
    LLM_DATA("llm_data", WsChannel.LLM),
    /**
     * @see com.openapi.domain.dto.ws.response.LlmEndResponse
     */
    LLM_END("llm_end", WsChannel.LLM),
    /**
     * @see com.openapi.domain.dto.ws.response.LlmErrorResponse
     */
    LLM_ERROR("llm_error", WsChannel.LLM),

    /// TTS 语音合成
    /**
     * @see com.openapi.domain.dto.ws.response.TtsStartResponse
     */
    TTS_START("tts_start", WsChannel.TTS),
    /**
     * 载荷见 {@link com.openapi.domain.dto.ws.response.TtsDataResponse}（含字幕字段 {@link com.openapi.domain.dto.ws.response.TtsDataResponse#getText()}）。
     */
    TTS_DATA("tts_data", WsChannel.TTS),
    /** 事件流 TTS 分片（带 instructionTiming/index）。 */
    TTS_EVENT("tts_event", WsChannel.TTS),
    /**
     * @see com.openapi.domain.dto.ws.response.TtsEndResponse
     */
    TTS_END("tts_end", WsChannel.TTS),
    /**
     * @see com.openapi.domain.dto.ws.response.TtsErrorResponse
     */
    TTS_ERROR("tts_error", WsChannel.TTS),

    /// VL 视觉理解
    /**
     * @see com.openapi.domain.dto.ws.request.VlStartRequest
     */
    VL_START("vl_start", WsChannel.VL),
    /**
     * @see com.openapi.domain.dto.ws.response.VlDataResponse
     */
    VL_DATA("vl_data", WsChannel.VL),
    /**
     * @see com.openapi.domain.dto.ws.response.VlEndResponse
     */
    VL_END("vl_end", WsChannel.VL),
    /**
     * @see com.openapi.domain.dto.ws.response.VlErrorResponse
     */
    VL_ERROR("vl_error", WsChannel.VL),

    /// 控制命令
    /**
     * @see com.openapi.domain.dto.ws.request.ControlCommandRequest
     */
    CONTROL_COMMAND_AN("control_command_an", WsChannel.CONTROL),
    /**
     * 服务端发往 Android / RK；由 {@link com.openapi.domain.dto.ws.response.ControlCommandResponse#target}
     * 区分推送目标，取值见 {@link WsPushTarget}。
     *
     * @see com.openapi.domain.dto.ws.response.ControlCommandResponse
     */
    CONTROL_COMMAND_SB("control_command_sb", WsChannel.CONTROL),
    /**
     * @see com.openapi.domain.dto.ws.request.CommandResultRequest
     */
    COMMAND_RESULT("command_result", WsChannel.CONTROL),
    /**
     * @see com.openapi.domain.dto.ws.response.ControlResponse
     */
    CONTROL_RESPONSE("control_response", WsChannel.CONTROL),
    /** 事件流 MCP 事件（带 instructionTiming/index）。 */
    MCP_EVENT("mcp_event", WsChannel.CONTROL),

    /// 指令批量编排
    /**
     * @see com.openapi.domain.dto.ws.response.InstructionListResponse
     * @see com.openapi.domain.dto.ws.response.InstructionTtsItem
     * @see com.openapi.domain.dto.ws.response.InstructionMcpItem
     * @see com.openapi.domain.constant.ws.Instruction
     */
    INSTRUCTION_LIST("instruction_list", WsChannel.INSTRUCTION_LIST),
    /**
     * @see com.openapi.domain.dto.ws.request.InstructionResultRequest
     * @see com.openapi.domain.constant.ws.Instruction
     */
    INSTRUCTION_RESULT("instruction_result", WsChannel.INSTRUCTION_LIST),

    /// 设备状态
    /**
     * @see com.openapi.domain.dto.ws.request.StatusRequest
     */
    STATUS_REQUEST("status_request", WsChannel.STATUS),
    /**
     * @see com.openapi.domain.dto.ws.response.RkStatusResponse
     */
    RK_STATUS("rk_status", WsChannel.STATUS),

    /// 系统消息
    /**
     * @see com.openapi.domain.dto.ws.response.ErrorResponse
     */
    ERROR("error", WsChannel.SYSTEM),
    /**
     * @see com.openapi.domain.dto.ws.response.SystemMessageResponse
     */
    SYSTEM_MESSAGE("system_message", WsChannel.SYSTEM),
    /** 事件流单条执行回执。 */
    INSTRUCTION_EVENT_RESULT("instruction_event_result", WsChannel.SYSTEM);

    @Getter
    private final String value;

    @Getter
    private final WsChannel channel;

    WsEvent(String value, WsChannel channel) {
        this.value = value;
        this.channel = channel;
    }

    /**
     * 根据字符串值获取枚举
     */
    @NonNull
    public static WsEvent fromValue(String value) {
        for (WsEvent event : values()) {
            if (event.value.equals(value)) {
                return event;
            }
        }
        return NONE;
    }
}
