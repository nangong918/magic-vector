package com.openapi.service.impl.tools;

import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.component.manager.realTimeChat.RealtimeChatContextManager;
import com.openapi.component.manager.realTimeChat.VLContext;
import com.openapi.domain.ao.mixLLM.McpSwitch;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.constant.realtime.RealtimeSystemResponseEventEnum;
import com.openapi.domain.dto.ws.response.SystemTextResponse;
import com.openapi.domain.evnet.TakePhotoEvent;
import com.openapi.domain.evnet.body.TakePhotoEventBody;
import com.openapi.domain.exception.AppException;
import com.openapi.service.model.VLService;
import com.openapi.service.tools.VisionToolService;
import com.openapi.config.SessionConfig;
import lombok.NonNull;
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
    private final VLService vlService;

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

        RealtimeChatContextManager chatContextManager = Optional.ofNullable(sessionConfig.realtimeChatContextManagerMap())
                .map(map -> map.get(agentId))
                .orElse(null);

        VLContext vlManager = sessionConfig.getVLManager(agentId);

        if (chatContextManager == null || vlManager == null) {
            throw new AppException(AgentExceptions.SESSION_NOT_EXIST);
        }

        // 没有缓存数据就请求前端发送一张照片
        if (vlManager.isEmpty()) {
            // 编辑调用请求
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
            eventPublisher.publishEvent(takePhotoEvent);
            log.info("[visionTool] 发送TakePhotoEvent: {}, 开始阻塞", takePhotoEvent.getEventBody());

            // 发送之后阻塞，设置超时30m，等待回复: 1. 如果获取到结果，结束阻塞。2.超过30_000Lms未获取到结果就直接返回告诉视觉处理超时适当回复。
            long startTime = System.currentTimeMillis();
            try {
                while (System.currentTimeMillis() - startTime < ModelConstant.VISION_TIMEOUT_MILLIS) {
                    // 检查是否有结果
                    String visionResult = chatContextManager.llmProxyContext.getVlContext().getVisionResult();
                    if (visionResult != null && !visionResult.trim().isEmpty()) {
                        log.info("[visionTool] 获取到视觉处理结果: {}", visionResult);
                        // 清空结果，避免影响下一次调用
                        chatContextManager.llmProxyContext.getVlContext().setVisionResult(visionResult);
                        return visionResult;
                    }

                    // 休眠一段时间再检查，避免过度消耗CPU
                    Thread.sleep(50);
                }

                // 超时处理
                log.warn("[visionTool] 视觉处理超时，等待时间超过 {} ms", ModelConstant.VISION_TIMEOUT_MILLIS);
                return "视觉处理超时，请稍后重试。";

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[visionTool] 等待视觉结果时被中断", e);
                return "视觉处理被中断，请稍后重试。";
            }
        }
        // 有缓存直接调用缓存
        else {
            try {
                if (vlManager.videoBase64 != null && !vlManager.videoBase64.isEmpty()) {
                    return vlService.vlVideoBase64(vlManager.videoBase64, chatContextManager.getUserRequestQuestion());
                }
                else {
                    return vlService.vlListFileBase64(vlManager.imagesBase64, chatContextManager.getUserRequestQuestion());
                }
            } catch (UploadFileException upe) {
                log.error("[visionTool] 文件上传失败", upe);
                return "文件上传失败，请稍后重试。";
            } catch (Exception e) {
                log.error("[visionTool] 模型调用失败，系统异常", e);
                return "模型调用失败，系统异常";
            } finally {
                // 清空vision资源
                vlManager.resetAll();
            }
        }
    }

    // 检查mcpSwitch状态
    private boolean checkIsForbid(@NonNull RealtimeChatContextManager contextManager){
        return McpSwitch.McpSwitchMode.CLOSE.code.equals(contextManager.getMcpSwitch().camera);
    }
}
