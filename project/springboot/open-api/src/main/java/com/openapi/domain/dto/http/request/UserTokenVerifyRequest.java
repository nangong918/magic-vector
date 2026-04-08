package com.openapi.domain.dto.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UserTokenVerifyRequest {
    @NotNull(message = "userId不能为空")
    @Positive(message = "userId必须大于0")
    private Long userId;

    @NotBlank(message = "accessToken不能为空")
    private String accessToken;
}
