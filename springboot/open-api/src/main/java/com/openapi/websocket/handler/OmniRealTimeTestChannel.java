package com.openapi.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.realtime.RealtimeDataTypeEnum;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author 13225
 * @date 2025/10/7 16:45
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ServerEndpoint(value = "/omni-realtime-test")
public class OmniRealTimeTestChannel {

    private Session session;

    private final ChatConfig config;

    // 连接打开
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        log.info("[websocket] 创建连接：id={}", session.getId());
    }

    // 连接关闭
    @OnClose
    public void onClose(CloseReason closeReason){
        log.info("[websocket] 连接断开：id={}，reason={}", this.session.getId(),closeReason);
    }

    // 连接异常
    @OnError
    public void onError(Throwable throwable) throws IOException {

        log.info("[websocket] 连接异常：id={}，throwable={}", this.session.getId(), throwable.getMessage());

        // 关闭连接。状态码为 UNEXPECTED_CONDITION（意料之外的异常）
        this.session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        // fastJson
        Map<String, String> messageMap = JSON.parseObject(message, new TypeReference<>() {});
        String type = messageMap.get(RealtimeDataTypeEnum.TYPE);
        if (!StringUtils.hasText(type)){
            log.warn("[websocket warn] 收到消息，类型为空");
            return;
        }
        RealtimeDataTypeEnum realtimeDataTypeEnum = RealtimeDataTypeEnum.getByType(type);
        switch (realtimeDataTypeEnum){
            case RealtimeDataTypeEnum.START_RECORDING -> {

            }
            case RealtimeDataTypeEnum.STOP_RECORDING -> {

            }
            case RealtimeDataTypeEnum.AUDIO_CHUNK -> {

            }
            case RealtimeDataTypeEnum.TEXT_MESSAGE -> {

            }
            default -> {
                log.warn("[websocket warn] 忽略未知类型消息：{}", type);
            }
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer message) throws IOException {
        log.info("[websocket] 收到二进制消息，长度：{}", message.remaining());
    }

}
