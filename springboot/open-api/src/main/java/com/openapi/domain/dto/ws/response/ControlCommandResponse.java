package com.openapi.domain.dto.ws.response;

import com.openapi.domain.dto.ws.base.CommonResultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ControlCommandResponse extends CommonResultDto {
    private String commandId;
    private String command;
    private Map<String, String> params;
}