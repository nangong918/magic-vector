package com.openapi.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @author 13225
 * @date 2025/10/29 13:37
 */
public interface VisionToolService {
    // 识别意图，意图是进行视觉识别
    @Tool(description = "用于告知前端传递一张当前相机的照片。当用户问Agent看看当前摄像头前是什么，或者询问Agent能看到用户给Agent展示的物品的时候之类的问题，就调用此方法。")
    void tellFrontTakePhoto(
            @ToolParam(description = "agentId") String agentId,
            @ToolParam(description = "用户Id") String userId,
            @ToolParam(description = "消息Id") String messageId
    );
}
