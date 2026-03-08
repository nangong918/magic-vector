package com.openapi.domain.dto.resonse;

import lombok.Data;

/**
 * 登录/注册后返回的用户认证信息
 */
@Data
public class UserAuthResponse {
    private Long userId;
    private String account;
    private String name;
    private String avatarUrl;
    private String accessToken;
}
