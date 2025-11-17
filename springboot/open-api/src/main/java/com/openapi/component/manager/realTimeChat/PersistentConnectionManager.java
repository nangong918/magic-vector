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
    private final PersistentConnectionService persistentConnectionService;

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
    }

    @Override
    public void disconnect() {
        if (connectionSession.isConnected()){
            log.info("[WebSocketConnection] disconnect, id={}", connectionSession.getSessionId());
            connectionSession.close();
        }

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

        switch (dataTypeEnum) {
            case CONNECT -> persistentConnectionService.handleConnectMessage(
                requestMessage,
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
        }
    }

}
