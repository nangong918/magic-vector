package com.demo.domain.dto.http.resonse;

import lombok.Data;

@Data
public class UserAuthResponse {
    private Long userId;
    private String account;
    private String name;
    private String avatarUrl;
    private String accessToken;
}
