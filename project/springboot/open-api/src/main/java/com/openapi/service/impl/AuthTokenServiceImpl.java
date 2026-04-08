package com.openapi.service.impl;

import com.openapi.config.DebugConfig;
import com.openapi.service.AuthTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {
    private static final long ACCESS_TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    private final Map<String, TokenSession> tokenSessionMap = new ConcurrentHashMap<>();
    private final DebugConfig debugConfig;

    @NotNull
    @Override
    public String issueAccessToken(@NotNull Long userId) {
        long now = System.currentTimeMillis();
        String token = "at_" + UUID.randomUUID().toString().replace("-", "");
        tokenSessionMap.put(token, new TokenSession(userId, now + ACCESS_TOKEN_TTL_MS));
        return token;
    }

    @Override
    public boolean verifyAccessToken(Long userId, String accessToken) {
        if (userId == null || !StringUtils.hasText(accessToken)) {
            if (debugConfig.shouldLogTokenVerify()) {
                log.debug("[token/verify] invalid request params, userId={}, token={}", userId, maskToken(accessToken));
            }
            return false;
        }
        TokenSession session = tokenSessionMap.get(accessToken);
        if (session == null) {
            if (debugConfig.shouldLogTokenVerify()) {
                log.debug("[token/verify] no session for token={}, userId={}", maskToken(accessToken), userId);
            }
            return false;
        }
        if (!userId.equals(session.userId())) {
            if (debugConfig.shouldLogTokenVerify()) {
                log.debug("[token/verify] user mismatch, requestUserId={}, tokenUserId={}", userId, session.userId());
            }
            return false;
        }
        if (session.expireAt() < System.currentTimeMillis()) {
            tokenSessionMap.remove(accessToken);
            if (debugConfig.shouldLogTokenVerify()) {
                log.debug("[token/verify] token expired, userId={}, token={}", userId, maskToken(accessToken));
            }
            return false;
        }
        return true;
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "<empty>";
        }
        if (token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    private record TokenSession(Long userId, long expireAt) {
    }
}
