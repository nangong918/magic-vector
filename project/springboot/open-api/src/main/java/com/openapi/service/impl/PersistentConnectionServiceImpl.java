package com.openapi.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.TypeReference;
import com.openapi.component.manager.realTimeChat.RealtimeChatContextManager;
import com.openapi.config.SessionConfig;
import com.openapi.connect.websocket.manager.PersistentConnectMessageManager;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.constant.realtime.RealtimeSystemRequestEventEnum;
import com.openapi.domain.dto.ws.request.RealtimeChatBindChannelRequest;
import com.openapi.domain.dto.ws.request.McpSwitchRequest;
import com.openapi.domain.dto.ws.request.RealtimeChatConnectRequest;
import com.openapi.domain.dto.ws.request.UploadPhotoRequest;
import com.openapi.domain.exception.AppException;
import com.openapi.interfaces.connect.ConnectionSession;
import com.openapi.service.PersistentConnectionService;
import com.openapi.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 13225
 * @date 2025/11/13 17:44
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistentConnectionServiceImpl implements PersistentConnectionService {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final PersistentConnectMessageManager connectMessageManager;
    private final RealtimeChatService realtimeChatService;
    private final SessionConfig sessionConfig;
    private final DashScopeChatModel dashScopeChatModel;

    @Override
    public void handleConnectMessage(
            @NotNull String connectMessage,
            @NotNull AtomicReference<String> userIdR,
            @NotNull ConnectionSession connectionSession
    ) {
        try {
            RealtimeChatConnectRequest connectRequest = JSON.parseObject(connectMessage, RealtimeChatConnectRequest.class);
            if (!StringUtils.hasText(connectRequest.getUserId())){
                throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
            }
            userIdR.set(connectRequest.getUserId());
            log.info("[PersistentConnection] user连接成功, userId: {}", userIdR.get());
        } catch (Exception e) {
            log.error("[PersistentConnection] 连接错误 断开连接", e);
            connectionSession.close();
        }
    }

    @Override
    public void handleBindChannelMessage(
            @NotNull String bindChannelMessage,
            @NotNull AtomicReference<String> userIdR,
            @NotNull AtomicReference<String> agentIdR,
            @NotNull ConnectionSession connectionSession
    ) {
        try {
            RealtimeChatBindChannelRequest bindRequest = JSON.parseObject(bindChannelMessage, RealtimeChatBindChannelRequest.class);
            if (!StringUtils.hasText(userIdR.get()) || !StringUtils.hasText(bindRequest.getAgentId())) {
                throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
            }
            var oldAgentId = agentIdR.get();
            if (StringUtils.hasText(oldAgentId)) {
                var oldContext = sessionConfig.realtimeChatContextManagerMap().remove(oldAgentId);
                if (oldContext != null) {
                    oldContext.reset();
                }
            }
            agentIdR.set(bindRequest.getAgentId());
            var contextManager = new RealtimeChatContextManager(connectMessageManager);
            contextManager.userId = userIdR.get();
            contextManager.agentId = bindRequest.getAgentId();
            contextManager.session = connectionSession;
            contextManager.connectTimestamp = bindRequest.getTimestamp();
            contextManager.chatClient = realtimeChatService.initChatClient(contextManager, dashScopeChatModel);
            sessionConfig.realtimeChatContextManagerMap().put(agentIdR.get(), contextManager);
            log.info("[PersistentConnection] bind channel success, userId={}, agentId={}", userIdR.get(), agentIdR.get());
        } catch (Exception e) {
            log.error("[PersistentConnection] bind channel error", e);
            connectionSession.close();
        }
    }

    @Override
    public void handleStartAudioRecordMessage(
            @NotNull String agentId
    ) {
        var contextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
        // 正在录音的话就返回
        if (contextManager.isRecording()){
            log.warn("[PersistentConnection] 录音已开始，前端录音申请驳回");
            return;
        }
        // 开启新的一问一答
        contextManager.newChatMessage();
        log.info("[PersistentConnection] 开始录音");

        var chatFuture = taskExecutor.submit(() -> {
            try {
                // 启动聊天
                realtimeChatService.startAudioChat(contextManager);
            } catch (Exception e) {
                log.error("[audioChat] 聊天处理异常", e);
                contextManager.endConversation();
            }
        });
        contextManager.addChatTask(chatFuture);
    }

    @Override
    public void handleStopAudioRecordMessage(
            @NotNull String agentId
    ) {
        var contextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
        log.info("[websocket] 停止录音");
        contextManager.stopRecord();
    }

    @Override
    public void handleAudioChunk(
            @NotNull String base64Audio,
            @NotNull String agentId
    ) {
        if (!StringUtils.hasText(base64Audio)){
            return;
        }
        var contextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
        // base64 -> byte[] (可能考虑需要使用异步, 如果录音数据并不是很影响写入就不异步了)
        byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
        contextManager.llmProxyContext.getSttRecordContext().offerAudioBuffer(audioBytes);
    }

    @Override
    public void handleUserTextMessage(
            @NotNull String userTextMessage,
            @NotNull String agentId
    ){
        var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
        log.info("[websocket] 收到文本消息：{}", userTextMessage);
        realtimeChatContextManager.newChatMessage();
        var chatFuture = taskExecutor.submit(() -> {
            try {
                // 启动text聊天
                realtimeChatService.startTextChat(
                        userTextMessage,
                        realtimeChatContextManager
                );
            } catch (Exception e) {
                log.error("[audioChat] 聊天处理异常", e);
                realtimeChatContextManager.endConversation();
            }
        });
        realtimeChatContextManager.addChatTask(chatFuture);
    }

    @Override
    public void handleSystemMessage(
            @NotNull String systemMessage,
            @NotNull String agentId
    ) throws JSONException{
        var contextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
        Map<String, String> responseMap = JSON.parseObject(systemMessage, new TypeReference<>() {});
        String eventType = responseMap.get(RealtimeSystemRequestEventEnum.EVENT_KET);
        RealtimeSystemRequestEventEnum eventEnum = RealtimeSystemRequestEventEnum.getByCode(eventType);

        switch (eventEnum) {
//            case UPLOAD_PHOTO -> handleUploadPhotoSystem(systemMessage, contextManager);
            case SUBMIT_MCP_SWITCH -> handleFunctionCallSystem(systemMessage, contextManager);
        }
    }

/*    private void handleUploadPhotoSystem(
            @NotNull String systemMessage,
            @NotNull RealtimeChatContextManager contextManager
    ) throws JSONException {
        UploadPhotoRequest systemRequest = JSON.parseObject(systemMessage, UploadPhotoRequest.class);
        if (systemRequest.isHavePhoto) {
            // 成功获取图片
            if (!systemRequest.isLastFragment && systemRequest.currentPhotoIndex != null) {
                // 不是最后一个碎片：拼接
                contextManager.llmProxyContext.getVlContext().imageBase64.append(systemRequest.photoBase64);
            }
            else {
                // 最后一个碎片：拼接完成
                contextManager.imageBase64.append(systemRequest.photoBase64);
                String imageBase64 = contextManager.imageBase64.toString();
                log.info("[websocket] 获取最后一个图片fragment成功，长度：{}", imageBase64.length());

                var visionChatFuture = taskExecutor.submit(() -> {
                    try {
                        // 启动vision聊天
                        realtimeChatService.startFunctionCallResultChat(
                                imageBase64,
                                contextManager,
                                true
                        );
                    } catch (Exception e) {
                        log.error("[vision chat] 聊天处理异常", e);
                        contextManager.endConversation();
                    }
                });
                contextManager.addFunctionCallTask(visionChatFuture);
            }
        }
        else {
            log.info("[websocket] 获取图片失败, 无照片");
            // 未获取到图片 -> 调用AI，告诉用户没有看到照片
        }
    }*/

    private void handleFunctionCallSystem(
            @NotNull String systemMessage,
            @NotNull RealtimeChatContextManager contextManager
    ) throws JSONException {
        McpSwitchRequest systemRequest = JSON.parseObject(systemMessage, McpSwitchRequest.class);
        Optional.ofNullable(systemRequest)
                .map(it -> it.mcpSwitch)
                .ifPresent(it -> contextManager.mcpSwitch.setByThat(it));
    }

}
