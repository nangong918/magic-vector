package com.openapi.domain.dto.ws.request;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ControlCommandRequest extends CommonResultDto {
    private String deviceId;
    private String command;
    private Map<String, String> params;
}