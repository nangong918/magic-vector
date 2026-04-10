package com.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "demo.auth")
public class AuthRouteProperties {
    private List<String> includePaths = new ArrayList<>(List.of(
            "/agent/**",
            "/chat/**"
    ));

    private List<String> excludePaths = new ArrayList<>(List.of(
            "/user/login",
            "/user/register",
            "/user/token/verify",
            "/test/**",
            "/error"
    ));

    private String userIdHeader = "user_id";
    private String accessTokenHeader = "access_token";

    private Long touristUserId = 1L;
    private String touristAccessToken = "tourist";
    private List<String> touristForbiddenPaths = new ArrayList<>(List.of(
            "/oss/**",
            "/user/password/**"
    ));
}
