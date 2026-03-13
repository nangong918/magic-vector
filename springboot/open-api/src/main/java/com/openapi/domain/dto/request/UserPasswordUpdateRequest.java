package com.openapi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPasswordUpdateRequest {
    @NotBlank(message = "userId不能为空")
    private String userId;
    @NotBlank(message = "oldPassword不能为空")
    private String oldPassword;
    @NotBlank(message = "newPassword不能为空")
    private String newPassword;
}
