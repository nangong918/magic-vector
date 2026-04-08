package com.openapi.domain.dto.ws.base;

import lombok.Data;

@Data
public class CommonResultDto {
    private String requestId;    // 请求追踪ID
    private Integer code;        // 200=成功
    private String message;      // 描述信息
}