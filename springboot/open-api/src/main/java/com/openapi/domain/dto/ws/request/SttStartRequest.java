package com.openapi.domain.dto.ws.request;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SttStartRequest extends CommonResultDto {
    private String agentId;
    /**
     * 本轮是否需要等待 VL 结果再进入 LLM。
     * - true: STT 完成后等待 VL（或超时）
     * - false/null: 默认不等待，除非另有 vl_start 事件声明
     */
    private Boolean needVl;
}