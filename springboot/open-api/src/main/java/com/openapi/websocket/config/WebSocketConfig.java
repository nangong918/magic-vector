package com.openapi.websocket.config;

import com.openapi.config.ChatConfig;
import com.openapi.config.ThreadPoolConfig;
import com.openapi.service.OmniRealTimeNoVADTestService;
import com.openapi.websocket.handler.OmniRealTimeNoVADTestChannel;
import com.openapi.websocket.handler.TestChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @author 13225
 * @date 2025/10/9 18:27
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OmniRealTimeNoVADTestService omniRealTimeNoVADTestService;
    private final ThreadPoolConfig threadPoolConfig;

    /**
     * 注册 WebSocket 处理器
     * 使用new是因为为了避免单例注入，每个长乱接都应该不是单例而是创建新的实例
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TestChannel(), "/test-channel")
                .addHandler(new OmniRealTimeNoVADTestChannel(
                        omniRealTimeNoVADTestService,
                        threadPoolConfig.taskExecutor()
                ), "/realtime-no-vad-test")
                .setAllowedOrigins("*"); // 根据需要设置允许的源
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}
