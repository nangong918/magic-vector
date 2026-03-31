package com.openapi.domain.dto.ws.request;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConnectRequest extends CommonResultDto {
    private String userId;
}