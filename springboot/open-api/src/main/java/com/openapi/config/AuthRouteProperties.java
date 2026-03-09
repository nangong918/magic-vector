package com.openapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "openapi.auth")
public class AuthRouteProperties {
    /**
     * 需要鉴权的路由（可在 application.yml 覆盖）
     */
    private List<String> includePaths = new ArrayList<>(List.of(
            "/agent/**",
            "/chat/**"
    ));

    /**
     * 鉴权白名单路由（可在 application.yml 覆盖）
     */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/user/login",
            "/user/register",
            "/user/token/verify",
            "/test/**",
            "/error"
    ));

    private String userIdHeader = "user_id";
    private String accessTokenHeader = "access_token";
}
