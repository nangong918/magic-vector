package com.magicvector.domain.dto.ws.response

/**
 * 服务端经 [com.magicvector.domain.constant.ws.WsEvent.INSTRUCTION_LIST] 下发的批量指令列表载荷。
 */
data class InstructionListResponse(
    /** 本批唯一 id，与 [com.magicvector.domain.dto.ws.request.InstructionResultRequest.requestId] 对齐 */
    val requestId: String,
    val agentId: String = "",
    /**
     * JSON 数组字符串；每个元素为 [InstructionTtsItem] 或 [InstructionMcpItem] 形态，以字段 `type` 区分 [com.magicvector.domain.constant.ws.Instruction]。
     */
    val instructions: String,
)
