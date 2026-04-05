package com.openapi.domain.dto.ws.response;

import com.openapi.domain.constant.ws.Instruction;
import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * {@link InstructionListResponse#getInstructions()} 中 {@link Instruction#MCP} 列表项，
 * 字段语义对齐 {@link ControlCommandResponse}（不含下行 {@link ControlCommandResponse#getTarget()}），
 * 并增加编排用 {@link #index}、执行路由等。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InstructionMcpItem extends CommonResultDto {

    private Instruction type = Instruction.MCP;

    private Integer index;

    /**
     * 执行侧路由，如 android_local / rk_device（与 {@link ControlCommandResponse#getTarget()} 的推送目标语义不同）。
     */
    private String target;

    private String deviceId;

    private String commandId;

    private String command;

    private Map<String, String> params;
}
