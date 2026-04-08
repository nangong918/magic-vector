package com.openapi.service;

import org.jetbrains.annotations.NotNull;

public interface AuthTokenService {
    @NotNull String issueAccessToken(@NotNull Long userId);

    boolean verifyAccessToken(Long userId, String accessToken);
}
