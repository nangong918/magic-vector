package com.magicvector.domain.dto.ws.response

import com.magicvector.domain.constant.ws.Instruction
import com.magicvector.domain.dto.ws.base.CommonResultDto

/**
 * [InstructionListResponse.instructions] 中 [Instruction.MCP] 列表项；字段语义对齐 [ControlCommandResponse]（不含其 WS 下行 [ControlCommandResponse.target]）。
 */
data class InstructionMcpItem(
    val index: Int,
    val type: Instruction = Instruction.MCP,
    /** 执行侧路由，如 android_local / rk_device */
    val target: String,
    val deviceId: String? = null,
    val commandId: String,
    val command: String,
    val params: Map<String, String>,
    requestId: String = "",
    code: Int = 0,
    message: String = "",
) : CommonResultDto(requestId, code, message)
