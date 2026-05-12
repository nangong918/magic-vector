package com.demo.config;

import com.demo.webrtc.WebRtcSignalingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebRTC 信令 WebSocket 注册。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebRtcSignalingConfig implements WebSocketConfigurer {
    private final WebRtcSignalingHandler webRtcSignalingHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webRtcSignalingHandler, "/ws/webrtc")
                .setAllowedOriginPatterns("*");
    }
}
