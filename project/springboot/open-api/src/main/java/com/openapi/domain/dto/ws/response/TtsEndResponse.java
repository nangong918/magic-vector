package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TtsEndResponse extends CommonResultDto {
    private String agentId;
}