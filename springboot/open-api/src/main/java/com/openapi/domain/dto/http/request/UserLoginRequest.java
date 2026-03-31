package com.openapi.domain.dto.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserLoginRequest {
    @NotBlank(message = "account不能为空")
    @Size(max = 64, message = "account长度不能超过64")
    private String account;

    @NotBlank(message = "password不能为空")
    @Size(min = 6, max = 64, message = "password长度应在6到64之间")
    private String password;
}
