package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class UserTokenVerifyResponse {
    private Boolean valid;
    private String message;
}
