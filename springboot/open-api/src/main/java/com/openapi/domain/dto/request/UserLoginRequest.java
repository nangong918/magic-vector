package com.openapi.domain.dto.request;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String account;
    private String password;
}
