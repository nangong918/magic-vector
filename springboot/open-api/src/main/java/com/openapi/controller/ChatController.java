package com.openapi.controller;


import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.resonse.ChatMessageResponse;
import com.openapi.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatMessageService chatMessageService;


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

}
