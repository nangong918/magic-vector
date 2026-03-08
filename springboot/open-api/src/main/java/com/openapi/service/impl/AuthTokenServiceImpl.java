package com.openapi.service.impl;

import com.openapi.service.AuthTokenService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级 access_token 管理服务（当前仅内存实现）。
 * TODO 后续可迁移到 Redis，并支持多端会话管理与主动失效。
 */
@Service
public class AuthTokenServiceImpl implements AuthTokenService {
    private static final long ACCESS_TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    private final Map<String, TokenSession> tokenSessionMap = new ConcurrentHashMap<>();

    @NotNull
    @Override
    public String issueAccessToken(@NotNull String userId) {
        long now = System.currentTimeMillis();
        String token = "at_" + UUID.randomUUID().toString().replace("-", "");
        tokenSessionMap.put(token, new TokenSession(userId, now + ACCESS_TOKEN_TTL_MS));
        return token;
    }

    @Override
    public boolean verifyAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }
        TokenSession session = tokenSessionMap.get(accessToken);
        if (session == null) {
            return false;
        }
        if (session.expireAt() < System.currentTimeMillis()) {
            tokenSessionMap.remove(accessToken);
            return false;
        }
        return true;
    }

    private record TokenSession(String userId, long expireAt) {
    }
}
