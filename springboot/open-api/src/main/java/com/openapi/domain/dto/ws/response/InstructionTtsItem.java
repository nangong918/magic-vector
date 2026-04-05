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

    private Integer index;
}
