package com.openapi.domain.dto.ws.request;

import com.openapi.domain.constant.ws.Instruction;
import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户端经 {@link com.openapi.domain.constant.ws.WsEvent#INSTRUCTION_RESULT} 上报的单步执行结果。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InstructionResultRequest extends CommonResultDto {

    /**
     * 对应 {@link InstructionListResponse} 内 {@code instructions} 中的序号，0-based。
     */
    private Integer index;

    /**
     * 与列表项 {@link com.openapi.domain.dto.ws.response.InstructionTtsItem#getType()} /
     * {@link com.openapi.domain.dto.ws.response.InstructionMcpItem#getType()} 一致。
     */
    private Instruction type;

    /**
     * 业务态，如 success / fail（具体取值由实现约定）。
     */
    private String status;
}
