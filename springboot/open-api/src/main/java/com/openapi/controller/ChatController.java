package com.openapi.controller;


import com.openapi.component.manager.realTimeChat.VLManager;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.resonse.ChatMessageResponse;
import com.openapi.service.ChatMessageService;
import com.openapi.service.RealtimeChatService;
import com.openapi.utils.FileUtils;
import com.openapi.config.SessionConfig;
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
import java.util.List;
import java.util.concurrent.ConcurrentMap;


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
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("agentId") String agentId,
            @RequestParam("userId") String userId,
            @RequestParam("messageId") String messageId
    ){
        if (images == null || images.isEmpty() || !StringUtils.hasText(agentId) || !StringUtils.hasText(userId) || !StringUtils.hasText(messageId)){
            log.warn("[uploadVisionImg] 参数错误, image: {}, agentId: {}, userId: {}, messageId: {}", images == null ? 0 : images.size(), agentId, userId, messageId);
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        ConcurrentMap<String, VLManager> vlManagerMap = sessionConfig.vlManagerMap();
        VLManager vlManager = vlManagerMap.get(agentId);
        if (vlManager == null) {
            log.warn("[uploadVisionImg] agentId: {} 不存在VLManager", agentId);
            return BaseResponse.LogBackError(AgentExceptions.SESSION_NOT_EXIST);
        }

        // 清空原先的数据 (不要勿清空video数据)
        vlManager.resetImages();

        // MultipartFile -> Base64
        for (MultipartFile image : images) {
            try {
                String base64Str = FileUtils.multipartFileToBase64(image);
                vlManagerMap.get(agentId).imagesBase64.add(base64Str);
            } catch (Exception e) {
                log.error("提供给前端上传视觉图片的接口：MultipartFile -> base64Str error: ", e);
                return BaseResponse.LogBackError(CommonExceptions.MULTIPART_FILE_TO_BASE64_ERROR);
            }
        }

        return BaseResponse.getResponseEntitySuccess("上传成功");
    }

}
