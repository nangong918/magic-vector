package com.openapi.controller;


import com.openapi.component.manager.realTimeChat.VLContext;
import com.openapi.converter.ChatMessageHttpConverter;
import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.request.ChatByAnchorRequest;
import com.openapi.domain.dto.resonse.ChatMessageDto;
import com.openapi.domain.dto.resonse.ChatMessageResponse;
import com.openapi.service.ChatMessageService;
import com.openapi.service.RealtimeChatService;
import com.openapi.utils.FileUtils;
import com.openapi.config.SessionConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final ChatMessageHttpConverter chatMessageHttpConverter;


    @GetMapping("/getLastChat")
    public BaseResponse<ChatMessageResponse> getLastChat(
            @RequestParam("agentId") String agentId
    ){
        // 参数校验
        if (!StringUtils.hasText(agentId)){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        Long agentIdLong = parseLong(agentId);
        if (agentIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        val chatMessageDos = chatMessageService.getLast10Messages(agentIdLong);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageList(chatMessageHttpConverter.doListToDtoList(chatMessageDos));

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
        Long agentIdLong = parseLong(agentId);
        if (agentIdLong == null) {
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

        val chatMessageDos = chatMessageService.getMessagesByAgentIdDeadlineLimit(agentIdLong, time, limit);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageList(chatMessageHttpConverter.doListToDtoList(chatMessageDos));

        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/getByAnchor")
    public BaseResponse<ChatMessageResponse> getByAnchor(
            @Valid @RequestBody ChatByAnchorRequest request
    ) {
        Integer limit = request.getLimit();
        Long agentIdLong = parseLong(request.getAgentId());
        if (agentIdLong == null || request.getAnchorTimestamp() == null
                || request.getBefore() == null || limit == null || limit <= 0) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (limit > ModelConstant.LIMIT_FETCH_CHAT_HISTORY_LENGTH) {
            limit = ModelConstant.LIMIT_FETCH_CHAT_HISTORY_LENGTH;
        }
        List<ChatMessageDo> chatMessageDos;
        if (request.getBefore()) {
            chatMessageDos = chatMessageService.getMessagesBeforeAnchorLimit(
                    agentIdLong,
                    request.getAnchorTimestamp(),
                    limit
            );
        } else {
            chatMessageDos = chatMessageService.getMessagesAfterAnchorLimit(
                    agentIdLong,
                    request.getAnchorTimestamp(),
                    limit
            );
        }
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageList(chatMessageHttpConverter.doListToDtoList(chatMessageDos));
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @GetMapping("/getListFull")
    public BaseResponse<ChatMessageResponse> getChatListFull(
            @RequestParam("agentId") String agentId,
            @RequestParam("userId") String userId
    ) {
        Long agentIdLong = parseLong(agentId);
        Long userIdLong = parseLong(userId);
        if (agentIdLong == null || userIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        List<ChatMessageDo> chatMessageDos = chatMessageService.getAllMessagesByAgentId(agentIdLong);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageList(chatMessageHttpConverter.doListToDtoList(chatMessageDos));
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @GetMapping("/getListPage")
    public BaseResponse<ChatMessageResponse> getChatListPage(
            @RequestParam("agentId") String agentId,
            @RequestParam("userId") String userId,
            @RequestParam("sortField") String sortField,
            @RequestParam("sortOrder") String sortOrder,
            @RequestParam("pageDirection") String pageDirection,
            @RequestParam("cursor") String cursor,
            @RequestParam("limit") Integer limit
    ) {
        Long agentIdLong = parseLong(agentId);
        Long userIdLong = parseLong(userId);
        if (agentIdLong == null || userIdLong == null || limit == null || limit <= 0) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!StringUtils.hasText(sortField) || !StringUtils.hasText(sortOrder) || !StringUtils.hasText(pageDirection)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        int safeLimit = Math.min(limit, ModelConstant.LIMIT_FETCH_CHAT_HISTORY_LENGTH);
        long cursorValue = parseCursor(cursor, "after".equalsIgnoreCase(pageDirection) ? 0L : Long.MAX_VALUE);

        List<ChatMessageDo> chatMessageDos;
        if ("after".equalsIgnoreCase(pageDirection)) {
            chatMessageDos = chatMessageService.getMessagesAfterAnchorLimit(agentIdLong, cursorValue, safeLimit);
        } else if ("before".equalsIgnoreCase(pageDirection)) {
            chatMessageDos = chatMessageService.getMessagesBeforeAnchorLimit(agentIdLong, cursorValue, safeLimit);
        } else {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        List<ChatMessageDto> messageList = chatMessageHttpConverter.doListToDtoList(chatMessageDos);
        Comparator<ChatMessageDto> comparator;
        if ("messageId".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparing(it -> parseLong(it.getMessageId()), Comparator.nullsLast(Long::compareTo));
        } else if ("timestamp".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparing(it -> parseLong(it.getTimestamp()), Comparator.nullsLast(Long::compareTo));
        } else {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        if ("DESC".equalsIgnoreCase(sortOrder)) {
            messageList = new ArrayList<>(messageList);
            messageList.sort(comparator.reversed());
        } else if ("ASC".equalsIgnoreCase(sortOrder)) {
            messageList = new ArrayList<>(messageList);
            messageList.sort(comparator);
        } else {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageList(messageList);
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

        VLContext vlManager = sessionConfig.getVLManager(agentId);
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
                vlManager.getImagesBase64().add(base64Str);
            } catch (Exception e) {
                log.error("提供给前端上传视觉图片的接口：MultipartFile -> base64Str error: ", e);
                return BaseResponse.LogBackError(CommonExceptions.MULTIPART_FILE_TO_BASE64_ERROR);
            }
        }

        return BaseResponse.getResponseEntitySuccess("上传成功");
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    private long parseCursor(String cursor, long defaultValue) {
        if (!StringUtils.hasText(cursor)) {
            return defaultValue;
        }
        Long parsed = parseLong(cursor);
        return parsed == null ? defaultValue : parsed;
    }

}
