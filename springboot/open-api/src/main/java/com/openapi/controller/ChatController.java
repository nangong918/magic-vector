package com.openapi.controller;


import com.alibaba.fastjson.JSON;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.constant.realtime.RealtimeResponseDataTypeEnum;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.resonse.ChatMessageResponse;
import com.openapi.service.ChatMessageService;
import com.openapi.service.RealtimeChatService;
import com.openapi.utils.FileUtils;
import com.openapi.websocket.config.SessionConfig;
import com.openapi.websocket.manager.WebSocketMessageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SessionConfig sessionConfig;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RealtimeChatService realtimeChatService;
    private final WebSocketMessageManager webSocketMessageManager;


    @GetMapping("/getLastChat")
    public BaseResponse<ChatMessageResponse> getLastChat(
            @RequestParam("agentId") String agentId
    ){
        // 参数校验
        if (!StringUtils.hasText(agentId)){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        val chatMessageDos = chatMessageService.getLast10Messages(agentId);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setChatMessages(chatMessageDos);

        return BaseResponse.getResponseEntitySuccess(response);
    }

    @GetMapping("/getTimeLimitChat")
    public BaseResponse<ChatMessageResponse> getTimeLimitChat(
            @RequestParam("agentId") String agentId,
            // yyyy-MM-dd HH:mm:ss
            @RequestParam("deadline") String deadline,
            // max 50
            @RequestParam("limit") Integer limit
    ){
        // 参数校验
        if (!StringUtils.hasText(agentId)){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        LocalDateTime time;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            time = LocalDateTime.parse(deadline, formatter);
        } catch (Exception e) {
            log.error("时间格式错误", e);
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (limit <= 0){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        else if (limit > ModelConstant.LIMIT_FETCH_CHAT_HISTORY_LENGTH){
            limit = ModelConstant.LIMIT_FETCH_CHAT_HISTORY_LENGTH;
        }

        val chatMessageDos = chatMessageService.getMessagesByAgentIdDeadlineLimit(agentId, time, limit);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setChatMessages(chatMessageDos);

        return BaseResponse.getResponseEntitySuccess(response);
    }

    /**
     * 提供给前端上传视觉图片的接口 （1.实现Http上传img然后调用）
     * 弃用使用SpringEvent传递Base64 since 2025/11/3;
     *     原因: 传递了MultipartFile还是需要SpringEvent传递给Service;
     *     SpringEvent是注册到内存，大文件会占用大量内存，可能导致 OOM;
     *     改进方案: 1. WebSocket分片上传JSON
     *     2. 用ConcurrentMap公开管理ChatContextManager和ChatClient，然后使用Http的方式进行调用
     */
    @PostMapping("/vision/upload/img")
    public BaseResponse<String> uploadVisionImg(
            @RequestParam("image") MultipartFile image,
            @RequestParam("agentId") String agentId,
            @RequestParam("userId") String userId,
            @RequestParam("messageId") String messageId
    ){
        if (image == null || image.isEmpty() || !StringUtils.hasText(agentId) || !StringUtils.hasText(userId) || !StringUtils.hasText(messageId)){
            log.warn("[websocket] 参数错误, image: {}, agentId: {}, userId: {}, messageId: {}", image, agentId, userId, messageId);
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        // MultipartFile -> Base64
        String base64Str;
        try {
            base64Str = FileUtils.multipartFileToBase64(image);
        } catch (Exception e) {
            log.error("提供给前端上传视觉图片的接口：MultipartFile -> base64Str error: ", e);
            return BaseResponse.LogBackError(CommonExceptions.MULTIPART_FILE_TO_BASE64_ERROR);
        }

        // 获取ChatClient和RealtimeChatContextManager
        val realtimeChatContextManager = sessionConfig.realtimeChatContextManagerMap().get(agentId);
        var visionChatFuture = taskExecutor.submit(() -> {
            try {
                // 取消之前的对话任务
                realtimeChatContextManager.cancelChatFuture();
//                realtimeChatContextManager.cancelVisionChatFuture();
                realtimeChatContextManager.cancelTtsFuture();
                // 启动vision聊天
                realtimeChatService.startVisionChat(
                        base64Str,
                        realtimeChatContextManager
                );
            } catch (Exception e) {
                realtimeChatContextManager.stopRecording.set(true);
                log.error("[vision chat] 聊天处理异常", e);
                Map<String, String> responseErrorMap = new HashMap<>();
                responseErrorMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.STOP_TTS.getType());
                responseErrorMap.put(RealtimeResponseDataTypeEnum.DATA, "聊天处理异常" + e.getMessage());
                String response = JSON.toJSONString(responseErrorMap);
                // 异常消息
                webSocketMessageManager.submitMessage(
                        agentId,
                        response
                );
            }
        });
        realtimeChatContextManager.setVisionChatFuture(visionChatFuture);

        return BaseResponse.getResponseEntitySuccess("上传成功");
    }

}
