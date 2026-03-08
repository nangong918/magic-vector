package com.openapi.domain.dto.resonse;

import lombok.Data;

@Data
public class UserTokenVerifyResponse {
    private Long userId;
    private Boolean valid;
    private String message;
}
