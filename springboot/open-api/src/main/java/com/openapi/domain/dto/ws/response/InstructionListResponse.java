package com.openapi.domain.dto.ws.response;

import lombok.Data;

/**
 * 服务端经 {@link com.openapi.domain.constant.ws.WsEvent#INSTRUCTION_LIST} 下发的批量指令列表载荷。
 */
@Data
public class InstructionListResponse {

    /**
     * 本批唯一 id，与 {@link com.openapi.domain.dto.ws.request.InstructionResultRequest#getRequestId()} 对齐。
     */
    private String requestId;

    private String agentId;

    /**
     * JSON 数组字符串；每个元素为 {@link InstructionTtsItem} 或 {@link InstructionMcpItem} 形态，
     * 以字段 {@code type} 区分 {@link com.openapi.domain.constant.ws.Instruction} 取值。
     */
    private String instructions;
}
