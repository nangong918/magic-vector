package com.openapi.connect.websocket.config;

import com.openapi.connect.websocket.handler.base.UnifiedWsHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author 13225
 * @date 2025/10/9 18:27
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final UnifiedWsHandler unifiedWsHandler;

    /**
     * 注册 WebSocket 处理器
     * 使用new是因为为了避免单例注入，每个长乱接都应该不是单例而是创建新的实例
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(unifiedWsHandler, "/unified")
                .setAllowedOrigins("*");
        // 其他测试端点可保留，但正式建议只保留统一端点
    }



}
