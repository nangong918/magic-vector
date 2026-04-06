package com.openapi.connect.websocket.router;

import com.google.gson.Gson;
import com.openapi.config.ThreadPoolConfig;
import com.openapi.connect.websocket.handler.AgentChannelHandler;
import com.openapi.connect.websocket.handler.ChatChannelHandler;
import com.openapi.connect.websocket.handler.ChannelHandler;
import com.openapi.connect.websocket.handler.ControlChannelHandler;
import com.openapi.connect.websocket.handler.ConnectChannelHandler;
import com.openapi.connect.websocket.handler.InstructionListChannelHandler;
import com.openapi.connect.websocket.handler.LlmChannelHandler;
import com.openapi.connect.websocket.handler.StatusChannelHandler;
import com.openapi.connect.websocket.handler.SttChannelHandler;
import com.openapi.connect.websocket.handler.SystemChannelHandler;
import com.openapi.connect.websocket.handler.TtsChannelHandler;
import com.openapi.connect.websocket.handler.VlChannelHandler;
import com.openapi.connect.websocket.manager.ConnectionManager;
import com.openapi.domain.constant.ws.WsChannel;
import com.openapi.domain.dto.ws.base.ClientEvent;
import com.openapi.domain.dto.ws.base.ServerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    private final Map<String, ChannelHandler> handlers = new ConcurrentHashMap<>();
    private final ConnectionManager connectionManager;
    private final ThreadPoolConfig threadPoolConfig;
    private final Gson gson;

    // 线程池（从 ThreadPoolConfig 获取）
    private ExecutorService businessExecutor;
    private ExecutorService sttExecutor;
    private ExecutorService controlExecutor;
    private ExecutorService instructionListExecutor;
    private ExecutorService llmExecutor;
    private ExecutorService ttsExecutor;
    private ExecutorService vlExecutor;

    @PostConstruct
    public void init() {
        // 初始化线程池
        businessExecutor = threadPoolConfig.businessExecutor();
        sttExecutor = threadPoolConfig.sttExecutor();
        controlExecutor = threadPoolConfig.controlExecutor();
        instructionListExecutor = threadPoolConfig.instructionListExecutor();
        llmExecutor = threadPoolConfig.llmExecutor();
        ttsExecutor = threadPoolConfig.ttsExecutor();
        vlExecutor = threadPoolConfig.vlExecutor();

        // 注册各个 ChannelHandler —— 注意：ping/pong 属于 connection 通道，不单独注册
        registerHandler(WsChannel.CONNECTION.getValue(), new ConnectChannelHandler(connectionManager, gson));
        registerHandler(WsChannel.AGENT.getValue(), new AgentChannelHandler());
        registerHandler(WsChannel.CHAT.getValue(), new ChatChannelHandler());
        registerHandler(WsChannel.STT.getValue(), new SttChannelHandler());
        registerHandler(WsChannel.LLM.getValue(), new LlmChannelHandler());
        registerHandler(WsChannel.TTS.getValue(), new TtsChannelHandler());
        registerHandler(WsChannel.VL.getValue(), new VlChannelHandler());
        registerHandler(WsChannel.CONTROL.getValue(), new ControlChannelHandler());
        registerHandler(WsChannel.INSTRUCTION_LIST.getValue(), new InstructionListChannelHandler());
        registerHandler(WsChannel.STATUS.getValue(), new StatusChannelHandler());
        registerHandler(WsChannel.SYSTEM.getValue(), new SystemChannelHandler());
        // 注意：没有 WsChannel.PING，因为 ping/pong 是 connection 通道的 event
    }

    public void registerHandler(String channel, ChannelHandler handler) {
        handlers.put(channel, handler);
    }

    public void route(WebSocketSession session, ClientEvent event) {
        String channel = event.getChannel();
        ChannelHandler handler = handlers.get(channel);
        if (handler == null) {
            log.warn("No handler for channel: {}", channel);
            sendError(session, "UNKNOWN_CHANNEL", "Channel not supported");
            return;
        }
        ExecutorService executor = selectExecutor(channel);
        executor.submit(() -> {
            try {
                handler.handle(session, event);
            } catch (Exception e) {
                log.error("Handler error for channel {}: {}", channel, e.getMessage(), e);
                sendError(session, "HANDLER_ERROR", e.getMessage());
            }
        });
    }

    private ExecutorService selectExecutor(String channelStr) {
        WsChannel channel = WsChannel.fromValue(channelStr);
        return switch (channel) {
            case STT -> sttExecutor;
            case CONTROL -> controlExecutor;
            case INSTRUCTION_LIST -> instructionListExecutor;
            case LLM -> llmExecutor;
            case TTS -> ttsExecutor;
            case VL -> vlExecutor;
            default -> businessExecutor;
        };
    }

    private void sendError(WebSocketSession session, String code, String message) {
        try {
            ServerEvent errorEvent = new ServerEvent(
                    WsChannel.SYSTEM.getValue(),
                    "error",
                    Map.of("code", code, "message", message)
            );
            session.sendMessage(new TextMessage(gson.toJson(errorEvent)));
        } catch (IOException e) {
            log.error("Failed to send error message", e);
        }
    }
}