package com.magicvector.domain.dto.ws.request

import com.magicvector.domain.constant.ws.Instruction
import com.magicvector.domain.dto.ws.base.CommonResultDto

/**
 * 客户端经 [com.magicvector.domain.constant.ws.WsEvent.INSTRUCTION_RESULT] 上报的单步执行结果。
 */
data class InstructionResultRequest(
    /** 对应 [com.magicvector.domain.dto.ws.response.InstructionListResponse.instructions] 内元素序号，0-based */
    val index: Int,
    /** 与列表项 [com.magicvector.domain.dto.ws.response.InstructionTtsItem.type] / [InstructionMcpItem.type] 一致 */
    val type: Instruction,
    /** 业务态，如 success / fail */
    val status: String,
    requestId: String = "",
    code: Int = 0,
    message: String = "",
) : CommonResultDto(requestId, code, message)
