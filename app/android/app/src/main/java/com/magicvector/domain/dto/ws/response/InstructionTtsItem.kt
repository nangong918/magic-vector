package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.constant.ws.Instruction

/**
 * [InstructionListResponse.instructions] 中 [Instruction.TTS] 列表项，字段与 [TtsDataResponse] 一致并增加编排序号。
 */
data class InstructionTtsItem(
    val index: Int,
    val type: Instruction = Instruction.TTS,
    val base64AudioStream: String,
    val text: String = "",
    val agentId: String = "",
    val seq: String = "",
    val isLast: Boolean = false,
    val timestamp: Long = 0L,
)
