package com.magicvector.domain.dto.http.response;

/**
 * 用户登录/注册响应
 */
public class UserAuthResponse {
    public Long userId;
    public String account;
    public String name;
    public String avatarUrl;
    public String accessToken;
}
