package com.openapi.listener;

import com.alibaba.fastjson.JSON;
import com.openapi.domain.constant.realtime.RealtimeResponseDataTypeEnum;
import com.openapi.domain.dto.ws.response.SystemTextResponse;
import com.openapi.domain.evnet.TakePhotoEvent;
import com.openapi.websocket.config.SessionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 13225
 * @date 2025/10/29 14:19
 * 处理Vision的事件；包括：图片，图片组，视频流
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class VisionEventListener {

    private final SessionConfig sessionConfig;

    @EventListener
    public void handleTakePhotoEvent(TakePhotoEvent event) {
        SystemTextResponse systemTextResponse = event.getEventBody().getSystemTextResponse();
        if (systemTextResponse.agentId == null){
            log.warn("[websocket warn] agentId == null");
            return;
        }

        // 在这里处理事件，例如进行视觉识别
        val session = sessionConfig.sessionMap().get(systemTextResponse.agentId);
        if (session == null){
            log.warn("[websocket warn] 找不到对应的会话：{}", systemTextResponse.agentId);
            return;
        }

        String response = JSON.toJSONString(systemTextResponse);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_SYSTEM_RESPONSE.getType());
        responseMap.put(RealtimeResponseDataTypeEnum.DATA, response);

        try {
            // 发送系统请求
            session.sendMessage(new TextMessage(JSON.toJSONString(responseMap)));
            log.info("[websocket] TakePhotoEvent 响应消息成功, agentId: {}", systemTextResponse.agentId);
        } catch (Exception e) {
            log.error("[websocket error] 响应消息异常", e);
        }
    }

}
