package com.openapi.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Slf4j
//@Configuration
public class WebSocket_Config {

    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        log.info("[websocket] 配置初始化");
        return new ServerEndpointExporter();
    }

}
