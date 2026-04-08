package com.magicvector.domain.constant.ws


enum class WsChannel(val value: String) {
    /**
     * 连接管理、心跳保活
     */
    CONNECTION("connection"),

    /**
     * Agent配置同步
     */
    AGENT("agent"),

    /**
     * 聊天消息传输
     */
    CHAT("chat"),

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
     * 指令批量编排（TTS + MCP 有序列表，与 [WsEvent.INSTRUCTION_RESULT] 成对）
     */
    INSTRUCTION_LIST("instruction_list"),

    /**
     * 设备状态上报与查询
     */
    STATUS("status"),

    /**
     * 系统消息与错误通知
     */
    SYSTEM("system")
}