package com.openapi.domain.dto.ws.request;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SttEndRequest extends CommonResultDto {
    private String agentId;
}