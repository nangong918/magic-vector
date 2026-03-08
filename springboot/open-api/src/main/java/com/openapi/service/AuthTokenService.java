package com.openapi.service;

import org.jetbrains.annotations.NotNull;

public interface AuthTokenService {
    @NotNull String issueAccessToken(@NotNull String userId);

    boolean verifyAccessToken(String accessToken);
}
