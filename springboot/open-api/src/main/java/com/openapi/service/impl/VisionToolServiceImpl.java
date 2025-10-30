package com.openapi.service.impl;

import com.openapi.domain.constant.realtime.RealtimeSystemEventEnum;
import com.openapi.domain.dto.ws.SystemTextResponse;
import com.openapi.domain.evnet.TakePhotoEvent;
import com.openapi.domain.evnet.body.TakePhotoEventBody;
import com.openapi.service.VisionToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author 13225
 * @date 2025/10/29 13:37
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionToolServiceImpl implements VisionToolService {

    private final ApplicationEventPublisher eventPublisher;

    // 识别意图，意图是进行视觉识别
    @Tool(description = "用于告知前端传递一张当前相机的照片。当用户问Agent看看当前摄像头前是什么，或者询问Agent能看到用户给Agent展示的物品的时候之类的问题，就调用此方法。")
    @Override
    public void tellFrontTakePhoto(
            @ToolParam(description = "agentId") String agentId,
            @ToolParam(description = "用户Id") String userId,
            @ToolParam(description = "消息Id") String messageId
    ) {
        SystemTextResponse systemTextResponse = new SystemTextResponse(
                agentId,
                userId,
                messageId,
                RealtimeSystemEventEnum.UPLOAD_PHOTO.getCode()
        );

        // 发送Spring Event
        TakePhotoEvent takePhotoEvent = new TakePhotoEvent(
                this,
                TakePhotoEventBody.builder()
                        .systemTextResponse(systemTextResponse)
                        .build()
        );

        eventPublisher.publishEvent(takePhotoEvent);
        log.info("[visionTool] 发送TakePhotoEvent: {}", takePhotoEvent);
    }
}
