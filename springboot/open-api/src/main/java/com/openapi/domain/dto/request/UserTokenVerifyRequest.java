package com.openapi.domain.dto.request;

import lombok.Data;

@Data
public class UserTokenVerifyRequest {
    private Long userId;
    private String accessToken;
}
