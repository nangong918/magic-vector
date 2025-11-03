package com.openapi.websocket.handler;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openapi.component.manager.RealtimeChatContextManager;
import com.openapi.domain.constant.realtime.RealtimeRequestDataTypeEnum;
import com.openapi.domain.constant.realtime.RealtimeResponseDataTypeEnum;
import com.openapi.domain.constant.realtime.RealtimeSystemRequestEventEnum;
import com.openapi.domain.dto.ws.request.RealtimeChatConnectRequest;
import com.openapi.domain.dto.ws.request.UploadPhotoRequest;
import com.openapi.service.RealtimeChatService;
import com.openapi.websocket.config.SessionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class RealtimeChatChannel extends TextWebSocketHandler {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final RealtimeChatService realtimeChatService;
    private final DashScopeChatModel dashScopeChatModel;
    private final SessionConfig sessionConfig;
    private String agentId;

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        log.info("[websocket] 创建连接：id={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        log.info("[websocket] 连接断开：id={}，reason={}", session.getId(), status);
        var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);

        // 取消聊天任务
        realtimeChatContextManager.cancelCurrentTask();

        // 清理资源
        sessionConfig.realtimeChatContextManagerMap().remove(agentId);
        agentId = null;
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, @NotNull Throwable exception) throws Exception {
        log.error("[websocket] 连接异常：id={}", session.getId(), exception);
//        session.close();
        sessionConfig.realtimeChatContextManagerMap().get(agentId).stopRecording.set(true);
        super.handleTransportError(session, exception);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws Exception {

        Map<String, String> messageMap = JSON.parseObject(message.getPayload(), new TypeReference<>() {});
        String type = messageMap.get(RealtimeRequestDataTypeEnum.TYPE);
        if (!StringUtils.hasText(type)) {
            log.warn("[websocket warn] 收到消息，类型为空");
            return;
        }

        // todo 处理Android传递的image信息，然后交给多模态，拿到消息之后将消息交还给chatModel然后进行返回
        RealtimeRequestDataTypeEnum realtimeDataTypeEnum = RealtimeRequestDataTypeEnum.getByType(type);
        switch (realtimeDataTypeEnum) {
            case CONNECT -> {
                String connectMessage = messageMap.get(RealtimeRequestDataTypeEnum.DATA);

                try {
                    RealtimeChatConnectRequest connectRequest = JSON.parseObject(connectMessage, RealtimeChatConnectRequest.class);
                    // 设置agentId
                    agentId = connectRequest.getAgentId();
                    log.info("[websocket] 连接，收集用户会话信息, agentId: {}", agentId);

                    // 重新连接就是新的会话信息
                    var realtimeChatContextManager = new RealtimeChatContextManager();
                    realtimeChatContextManager.userId = connectRequest.getUserId();
                    realtimeChatContextManager.agentId = connectRequest.getAgentId();
                    realtimeChatContextManager.session = session;
                    realtimeChatContextManager.connectTimestamp = connectRequest.getTimestamp();

                    // 初始化chatClient
                    // 将获取到的chatClient存储到RealtimeChatContextManager
                    realtimeChatContextManager.chatClient = realtimeChatService.initChatClient(realtimeChatContextManager, dashScopeChatModel);

                    // 将session存储到map
                    sessionConfig.realtimeChatContextManagerMap().put(agentId, realtimeChatContextManager);

                } catch (Exception e){
                    log.warn("[websocket warn] 断开连接，参数错误");
                    session.close();
                }
            }
            case START_AUDIO_RECORD -> {
                var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
                // 正在录音的话就返回
                if (!realtimeChatContextManager.stopRecording.get()){
                    log.warn("[websocket warn] 录音已开始，前端录音申请驳回");
                    return;
                }
                // 无VAD模式
                // 开启新的一问一答
                realtimeChatContextManager.newChatMessage();
                log.info("[websocket] 开始录音");
                realtimeChatContextManager.stopRecording.set(false);
                var chatFuture = taskExecutor.submit(() -> {
                    try {
                        // 启动聊天
                        realtimeChatService.startChat(realtimeChatContextManager);
                    } catch (Exception e) {
                        realtimeChatContextManager.stopRecording.set(true);
                        log.error("[audioChat] 聊天处理异常", e);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                        responseMap.put(RealtimeResponseDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException ex) {
                            log.error("[websocket error] 响应消息异常", ex);
                        }
                    }
                });
                realtimeChatContextManager.setChatFuture(chatFuture);
            }
            case STOP_AUDIO_RECORD -> {
                var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
                log.info("[websocket] 停止录音");
                realtimeChatContextManager.stopRecording.set(true);
            }
            case AUDIO_CHUNK -> {
                handleAudioChunk(messageMap.get(RealtimeRequestDataTypeEnum.DATA));
            }
            case USER_TEXT_MESSAGE -> {
                var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
                var chatClient = realtimeChatContextManager.chatClient;
                log.info("[websocket] 收到文本消息：{}", messageMap.get(RealtimeRequestDataTypeEnum.DATA).length());
                realtimeChatContextManager.newChatMessage();
                realtimeChatContextManager.stopRecording.set(false);
                var chatFuture = taskExecutor.submit(() -> {
                    try {
                        // 启动text聊天
                        realtimeChatService.startTextChat(
                                messageMap.get(RealtimeRequestDataTypeEnum.DATA),
                                realtimeChatContextManager
                        );
                    } catch (Exception e) {
                        realtimeChatContextManager.stopRecording.set(true);
                        log.error("[audioChat] 聊天处理异常", e);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                        responseMap.put(RealtimeResponseDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
                        String response = JSON.toJSONString(responseMap);
                        try {
                            session.sendMessage(new TextMessage(response));
                        } catch (IOException ex) {
                            log.error("[websocket error] 响应消息异常", ex);
                        }
                    }
                });
                realtimeChatContextManager.setChatFuture(chatFuture);
            }
            case SYSTEM_MESSAGE -> {
                var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
                var chatClient = realtimeChatContextManager.chatClient;
                String responseJson = messageMap.get(RealtimeRequestDataTypeEnum.DATA);
                log.info("[websocket] 收到前端System消息：{}", responseJson.length());
                // 系统消息 -> 拍摄了照片 / 拍摄照片失败
                try {
                    // 使用 TypeReference 指定 Map 的类型
                    Map<String, String> responseMap = JSON.parseObject(responseJson, new TypeReference<>() {});
                    String eventType = responseMap.get(RealtimeSystemRequestEventEnum.EVENT_KET);
                    // 实现websocket分片上传json，然后调用
                    if (eventType.equals(RealtimeSystemRequestEventEnum.UPLOAD_PHOTO.getCode())){
                        /**
                         * 获取图片Map<String, String>
                         * @see com.openapi.domain.dto.ws.request.UploadPhotoRequest
                         */
                        UploadPhotoRequest systemRequest = JSON.parseObject(responseJson, UploadPhotoRequest.class);
                        if (systemRequest.isHavePhoto) {
                            // 成功获取图片
                            if (!systemRequest.isLastFragment) {
                                // 不是最后一个碎片：拼接
                                realtimeChatContextManager.imageBase64.append(systemRequest.photoBase64);
                            }
                            else {
                                // 最后一个碎片：拼接完成
                                realtimeChatContextManager.imageBase64.append(systemRequest.photoBase64);
                                String imageBase64 = realtimeChatContextManager.imageBase64.toString();
                                log.info("[websocket] 获取最后一个图片fragment成功，长度：{}", imageBase64.length());

                                var visionChatFuture = taskExecutor.submit(() -> {
                                    try {
                                        // 启动vision聊天
                                        realtimeChatService.startVisionChat(
                                                imageBase64,
                                                realtimeChatContextManager
                                        );
                                    } catch (Exception e) {
                                        realtimeChatContextManager.stopRecording.set(true);
                                        log.error("[vision chat] 聊天处理异常", e);
                                        Map<String, String> responseErrorMap = new HashMap<>();
                                        responseErrorMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                                        responseErrorMap.put(RealtimeResponseDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
                                        String response = JSON.toJSONString(responseErrorMap);
                                        try {
                                            session.sendMessage(new TextMessage(response));
                                        } catch (IOException ex) {
                                            log.error("[websocket error] 响应消息异常", ex);
                                        }
                                    }
                                });
                                realtimeChatContextManager.setVisionChatFuture(visionChatFuture);
                            }
                        }
                        else {
                            log.info("[websocket] 获取图片失败, 无照片");
                            // 未获取到图片 -> 调用AI，告诉用户没有看到照片
                        }
                    }
                } catch (Exception e) {
                    log.error("[SYSTEM_MESSAGE] error 解析失败", e);
                }
            }
            default -> log.warn("[websocket warn] 忽略未知类型消息：{}", type);
        }
    }

    @Override
    protected void handleBinaryMessage(@NotNull WebSocketSession session, BinaryMessage message) {
        log.info("[websocket] 收到二进制消息，长度：{}", message.getPayloadLength());
    }

    private void handleAudioChunk(String base64Audio) {
        if (!StringUtils.hasText(base64Audio)){
            return;
        }
        // 解码 Base64 字符串为字节数组
        byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
//        log.info("[websocket] 音频块大小：{}", audioBytes.length);

        // 将字节数组放入队列
        var realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
        realtimeChatContextManager.requestAudioBuffer.offer(audioBytes);
    }
}
