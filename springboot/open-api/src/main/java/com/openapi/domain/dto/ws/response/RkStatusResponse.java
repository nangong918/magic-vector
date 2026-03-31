package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RkStatusResponse extends CommonResultDto {
    private String deviceId;
    private Integer battery;
    private String position;
}