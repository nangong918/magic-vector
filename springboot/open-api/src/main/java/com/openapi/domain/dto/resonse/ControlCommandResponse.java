package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class ControlCommandResponse {
    private Boolean accepted;
    private String traceId;
    private String message;
}
