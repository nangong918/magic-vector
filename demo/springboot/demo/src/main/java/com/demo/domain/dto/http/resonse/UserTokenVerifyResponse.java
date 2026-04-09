package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class UserTokenVerifyResponse {
    private Long userId;
    private Boolean valid;
    private String message;
}
