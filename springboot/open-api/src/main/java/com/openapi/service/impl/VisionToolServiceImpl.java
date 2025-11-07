package com.openapi.service.impl;

import com.openapi.component.manager.realTimeChat.RealtimeChatContextManager;
import com.openapi.domain.constant.realtime.RealtimeSystemResponseEventEnum;
import com.openapi.domain.dto.ws.response.SystemTextResponse;
import com.openapi.domain.evnet.TakePhotoEvent;
import com.openapi.domain.evnet.body.TakePhotoEventBody;
import com.openapi.service.VisionToolService;
import com.openapi.websocket.config.SessionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author 13225
 * @date 2025/10/29 13:37
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionToolServiceImpl implements VisionToolService {

    private final ApplicationEventPublisher eventPublisher;
    private final SessionConfig sessionConfig;

    // 识别意图，意图是进行视觉识别
    @Tool(description = """
            用于调用请求调用前端摄像头。
            当用户问Agent看看当前摄像头前是什么，或者询问Agent能看到用户给Agent展示的物品的时等涉及视觉任务调用此方法，
            此方法用于告知前端传递一张当前相机的照片。
            """)
    @Override
    public String tellFrontTakePhoto(
            @ToolParam(description = "agentId") String agentId,
            @ToolParam(description = "用户Id") String userId,
            @ToolParam(description = "消息Id") String messageId
    ) {
        log.info("[visionTool::tellFrontTakePhoto] Agent传入入参检查, agentId: {}, userId: {}, messageId: {}", agentId, userId, messageId);

        SystemTextResponse systemTextResponse = new SystemTextResponse(
                agentId,
                userId,
                messageId,
                RealtimeSystemResponseEventEnum.UPLOAD_PHOTO.getCode()
        );

        // 发送Spring Event
        TakePhotoEvent takePhotoEvent = new TakePhotoEvent(
                this,
                TakePhotoEventBody.builder()
                        .systemTextResponse(systemTextResponse)
                        .build()
        );

        RealtimeChatContextManager chatContextManager = Optional.ofNullable(sessionConfig.realtimeChatContextManagerMap())
                        .map(map -> map.get(agentId))
                        .orElse(null);

        // 设置识别正在进行function call
        if (chatContextManager != null){
            // 添加一条function call信号量
            chatContextManager.llmProxyContext.addFunctionCallRecord();
        }
        else {
            log.warn("未找到agentId: {}, 对应的chatContextManager", agentId);
        }

        eventPublisher.publishEvent(takePhotoEvent);
        log.info("[visionTool] 发送TakePhotoEvent: {}", takePhotoEvent.getEventBody());

        return "已经成功向前端发送调用接口请求，你现在只需要回复类似：“稍等，让我来看看”就好了";
    }
}
