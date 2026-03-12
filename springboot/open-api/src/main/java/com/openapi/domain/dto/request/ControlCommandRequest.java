package com.openapi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ControlCommandRequest {
    @NotBlank(message = "userId不能为空")
    private String userId;

    @NotBlank(message = "deviceId不能为空")
    private String deviceId;

    @NotBlank(message = "transport不能为空")
    private String transport;

    @NotBlank(message = "commandType不能为空")
    private String commandType;

    @NotNull(message = "sequence不能为空")
    private Long sequence;

    @NotNull(message = "timestamp不能为空")
    private Long timestamp;

    @NotBlank(message = "payloadJson不能为空")
    private String payloadJson;
}
