package com.openapi.domain.dto.ws.response;

import com.openapi.domain.constant.ws.Instruction;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {@link InstructionListResponse#getInstructions()} 中 {@link Instruction#TTS} 列表项，
 * 在 {@link TtsDataResponse} 基础上增加编排序号。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InstructionTtsItem extends TtsDataResponse {

    private Instruction type = Instruction.TTS;

    /** 本轮事件流批次id。 */
    private String requestId;

    private Integer index;

    /** 客户端排序阶段号：同 timing 可并行，跨 timing 串行。 */
    private Integer instructionTiming;
}
