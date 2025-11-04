package com.openapi.websocket.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.openapi.config.ThreadPoolConfig;
import com.openapi.service.test.OmniRealTimeNoVADTestService;
import com.openapi.service.test.RealTimeTestServiceService;
import com.openapi.service.RealtimeChatService;
import com.openapi.websocket.handler.test.OmniRealTimeNoVADTestChannel;
import com.openapi.websocket.handler.test.RealTimeTestChannel;
import com.openapi.websocket.handler.RealtimeChatChannel;
import com.openapi.websocket.handler.test.TestChannel;
import com.openapi.websocket.manager.WebSocketMessageManager;
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

    private final OmniRealTimeNoVADTestService omniRealTimeNoVADTestService;
    private final ThreadPoolConfig threadPoolConfig;
    private final RealTimeTestServiceService realTimeTestServiceService;
    private final RealtimeChatService realtimeChatService;
    private final DashScopeChatModel dashScopeChatModel;
    private final SessionConfig sessionConfig;
    private final WebSocketMessageManager webSocketMessageManager;

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
                .addHandler(new RealTimeTestChannel(
                                realTimeTestServiceService,
                                threadPoolConfig.taskExecutor(),
                                dashScopeChatModel
                        ),
                        "/realtime-test")
                .addHandler(new RealtimeChatChannel(
                                threadPoolConfig.taskExecutor(),
                                realtimeChatService,
                                dashScopeChatModel,
                                sessionConfig,
                                webSocketMessageManager
                        ),
                        "/agent/realtime/chat")
                .setAllowedOrigins("*"); // 根据需要设置允许的源
    }



}
