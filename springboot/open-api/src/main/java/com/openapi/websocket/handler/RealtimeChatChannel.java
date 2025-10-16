package com.openapi.websocket.handler;


import com.openapi.component.manager.RealtimeChatContextManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
@Component
public class RealtimeChatChannel extends TextWebSocketHandler {

    private final ThreadPoolTaskExecutor taskExecutor;
    private volatile Future<?> chatFuture;
    private RealtimeChatContextManager realtimeChatContextManager = new RealtimeChatContextManager();

}
