package com.openapi.service.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @author 13225
 * @date 2025/10/29 13:37
 */
public interface VisionToolService {
    // 识别意图，意图是进行视觉识别
    @Tool(description = """
            获取当前视觉的结果。当用户问你的问题设计到视觉任务就调用此方法，如果仅仅问你是否能听得到这类问题就无需调用。
            当用户问Agent看看当前摄像头前是什么，或者询问Agent能看到用户给Agent展示的物品的时等涉及视觉任务调用此方法。
            """)
    String getVisionResult(
            @ToolParam(description = "agentId") String agentId,
            @ToolParam(description = "用户Id") String userId
    );
}
