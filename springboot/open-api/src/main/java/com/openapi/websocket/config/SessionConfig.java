package com.openapi.websocket.config;

import com.openapi.component.manager.realTimeChat.RealtimeChatContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 13225
 * @date 2025/10/29 13:55
 */
@RequiredArgsConstructor
@Configuration
public class SessionConfig {

    // ConcurrentMap<agentId, RealtimeChatContextManager> (不安全的，异常断开的时候不会删除agentId，每次调用需要try)
    private final ConcurrentMap<String, RealtimeChatContextManager> realtimeChatContextManagerMap = new ConcurrentHashMap<>();

    @Bean("realtimeChatContextManagerMap")
    public ConcurrentMap<String, RealtimeChatContextManager> realtimeChatContextManagerMap() {
        return realtimeChatContextManagerMap;
    }

}
