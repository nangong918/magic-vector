package com.magicvector.domain.constant.ws

import com.google.gson.annotations.SerializedName

/**
 * `instruction_list` 列表项及 [com.magicvector.domain.dto.ws.request.InstructionResultRequest] 中的类型判别，
 * 与 JSON 中的 `type` 字段对应。
 */
enum class Instruction(val value: String) {
    /** 语音合成播放项 */
    @SerializedName("tts")
    TTS("tts"),

    /** MCP / 控制执行项 */
    @SerializedName("mcp")
    MCP("mcp");

    companion object {
        fun fromValue(raw: String?): Instruction? {
            if (raw.isNullOrEmpty()) return null
            return entries.find { it.value == raw }
        }
    }
}
