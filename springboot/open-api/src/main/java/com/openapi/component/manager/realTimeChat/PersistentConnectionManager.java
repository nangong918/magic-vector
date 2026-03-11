package com.openapi.component.manager.realTimeChat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.TypeReference;
import com.openapi.config.SessionConfig;
import com.openapi.domain.constant.realtime.RealtimeRequestDataTypeEnum;
import com.openapi.interfaces.connect.ConnectionSession;
import com.openapi.interfaces.connect.IPersistentConnectionManager;
import com.openapi.interfaces.connect.Message;
import com.openapi.service.PersistentConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @author 13225
 * @date 2025/11/13 17:52
 * 连接管理器
 * 设计模式：适配器，代理，抽象工厂
 * 抽象工厂：分别对Mqtt和Ws进行抽象，抽象出基本功能分别创建，
 * 代理：ConnectSession代理Mqtt和Ws的发送和接收消息
 * 适配器：将Mqtt和Ws的连接逻辑进行适配，同一个PersistentConnectionManager根据不同的对象进行适配对象，
 */
@Slf4j
public class PersistentConnectionManager implements IPersistentConnectionManager {

    private ConnectionSession connectionSession;

    private final SessionConfig sessionConfig;
    private final AtomicReference<String> agentId = new AtomicReference<>(null);
    private final AtomicReference<String> userId = new AtomicReference<>(null);
    private final AtomicReference<Long> lastHeartbeatTs = new AtomicReference<>(0L);
    private final ScheduledExecutorService heartbeatChecker = Executors.newSingleThreadScheduledExecutor();
    private final PersistentConnectionService persistentConnectionService;
    private static final long HEARTBEAT_TIMEOUT_MS = 60_000L;

    public PersistentConnectionManager(
            SessionConfig sessionConfig,
            PersistentConnectionService persistentConnectionService
    ){
        this.sessionConfig = sessionConfig;
        this.persistentConnectionService = persistentConnectionService;
    }

    @Override
    public void connect(ConnectionSession connectionSession) {
        // WebSocket 连接逻辑（通常由前端或客户端发起）
        log.info("[WebSocketConnection] connect, id={}", connectionSession.getSessionId());
        this.connectionSession = connectionSession;
        lastHeartbeatTs.set(System.currentTimeMillis());
        heartbeatChecker.scheduleWithFixedDelay(() -> {
            try {
                if (!connectionSession.isConnected()) {
                    return;
                }
                long now = System.currentTimeMillis();
                long last = lastHeartbeatTs.get();
                if (last > 0 && now - last > HEARTBEAT_TIMEOUT_MS) {
                    log.warn("[WebSocketConnection] heartbeat timeout, disconnect session={}", connectionSession.getSessionId());
                    disconnect();
                }
            } catch (Exception e) {
                log.error("[WebSocketConnection] heartbeat checker error", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void disconnect() {
        if (connectionSession.isConnected()){
            log.info("[WebSocketConnection] disconnect, id={}", connectionSession.getSessionId());
            connectionSession.close();
        }
        heartbeatChecker.shutdownNow();

        var agentIdStr = agentId.get();
        if (agentIdStr != null) {
            var contextManager = sessionConfig.realtimeChatContextManagerMap().get(agentIdStr);
            if (contextManager != null){
                // 取消聊天任务
                contextManager.reset();
                // 清理资源
                sessionConfig.realtimeChatContextManagerMap().remove(agentIdStr);
            }
        }

        agentId.set(null);
        userId.set(null);
        lastHeartbeatTs.set(0L);
    }

    @Override
    public void onThrowable(Throwable throwable) {
        log.error("[WebSocketConnection] onThrowable error, id={}", connectionSession.getSessionId(), throwable);
        if (agentId.get() != null){
            sessionConfig.realtimeChatContextManagerMap().remove(agentId.get());
        }
    }

    @Override
    public void onMessage(Message message) {
        Map<String, String> messageMap = JSON.parseObject(message.getPayload(), new TypeReference<>() {});
        String type = messageMap.get(RealtimeRequestDataTypeEnum.TYPE);

        if (!StringUtils.hasText(type)) {
            log.warn("[WebSocketConnection] 收到消息，类型为空");
            return;
        }

        RealtimeRequestDataTypeEnum dataTypeEnum = RealtimeRequestDataTypeEnum.getByType(type);
        var requestMessage = messageMap.get(RealtimeRequestDataTypeEnum.DATA);
        if (dataTypeEnum != RealtimeRequestDataTypeEnum.CONNECT
                && dataTypeEnum != RealtimeRequestDataTypeEnum.BIND_CHANNEL
                && dataTypeEnum != RealtimeRequestDataTypeEnum.HEARTBEAT
                && !StringUtils.hasText(agentId.get())) {
            log.warn("[WebSocketConnection] no bound channel, type={}", dataTypeEnum);
            return;
        }

        switch (dataTypeEnum) {
            case CONNECT -> persistentConnectionService.handleConnectMessage(
                requestMessage,
                userId,
                connectionSession,
                lastHeartbeatTs
            );
            case BIND_CHANNEL -> persistentConnectionService.handleBindChannelMessage(
                requestMessage,
                userId,
                agentId,
                connectionSession
            );
            case START_AUDIO_RECORD -> persistentConnectionService.handleStartAudioRecordMessage(
                agentId.get()
            );
            case STOP_AUDIO_RECORD -> persistentConnectionService.handleStopAudioRecordMessage(
                agentId.get()
            );
            case AUDIO_CHUNK -> persistentConnectionService.handleAudioChunk(
                requestMessage,
                agentId.get()
            );
            case USER_TEXT_MESSAGE -> persistentConnectionService.handleUserTextMessage(
                requestMessage,
                agentId.get()
            );
            case SYSTEM_MESSAGE -> {
                try {
                    persistentConnectionService.handleSystemMessage(
                            requestMessage,
                            agentId.get()
                    );
                } catch (JSONException e) {
                    log.error("[WebSocketConnection] handleSystemMessage error, id={}", connectionSession.getSessionId(), e);
                }
            }
            case HEARTBEAT -> persistentConnectionService.handleHeartbeat(lastHeartbeatTs);
        }
    }

}
