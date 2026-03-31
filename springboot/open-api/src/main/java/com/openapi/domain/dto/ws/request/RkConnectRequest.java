package com.openapi.domain.dto.ws.request;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RkConnectRequest extends CommonResultDto {
    private String deviceId;
    private String firmware;
}