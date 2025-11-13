package com.openapi.component.manager.realTimeChat;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openapi.config.SessionConfig;
import com.openapi.connect.websocket.manager.WebSocketMessageManager;
import com.openapi.domain.constant.realtime.RealtimeRequestDataTypeEnum;
import com.openapi.interfaces.connect.IPersistentConnectionManager;
import com.openapi.interfaces.connect.Message;
import com.openapi.interfaces.connect.PersistentConnection;
import com.openapi.service.RealtimeChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

import java.util.Map;


/**
 * @author 13225
 * @date 2025/11/13 17:52
 */
@Slf4j
public class PersistentConnectionManager implements IPersistentConnectionManager {

    private PersistentConnection connection;

    private final ThreadPoolTaskExecutor taskExecutor;
    private final RealtimeChatService realtimeChatService;
    private final DashScopeChatModel dashScopeChatModel;
    private final SessionConfig sessionConfig;
    private String agentId;
    private final WebSocketMessageManager webSocketMessageManager;

    public PersistentConnectionManager(
            ThreadPoolTaskExecutor taskExecutor,
            RealtimeChatService realtimeChatService,
            DashScopeChatModel dashScopeChatModel,
            SessionConfig sessionConfig,
            WebSocketMessageManager webSocketMessageManager
    ){
        this.taskExecutor = taskExecutor;
        this.realtimeChatService = realtimeChatService;
        this.dashScopeChatModel = dashScopeChatModel;
        this.sessionConfig = sessionConfig;
        this.webSocketMessageManager = webSocketMessageManager;
    }

    @Override
    public void connect(PersistentConnection connection) {
        // WebSocket 连接逻辑（通常由前端或客户端发起）
        log.info("[WebSocketConnection] connect, id={}", connection.getSession().getSessionId());
        this.connection = connection;
    }

    @Override
    public void disconnect() {
        if (connection.getSession().isConnected()){
            log.info("[WebSocketConnection] disconnect, id={}", connection.getSession().getSessionId());
            connection.getSession().close();
        }

        var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);

        // 取消聊天任务
        realtimeChatContextManager.reset();

        // 清理资源
        sessionConfig.realtimeChatContextManagerMap().remove(agentId);
        agentId = null;
    }

    @Override
    public void onThrowable(Throwable throwable) {
        log.error("[WebSocketConnection] onThrowable error, id={}", connection.getSession().getSessionId(), throwable);
        sessionConfig.realtimeChatContextManagerMap().get(agentId).reset();
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


    }

}
