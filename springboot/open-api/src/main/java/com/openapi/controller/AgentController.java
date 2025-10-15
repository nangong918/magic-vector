package com.openapi.controller;

import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.ao.AgentChatAo;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.constant.error.UserExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.resonse.AgentLastChatListResponse;
import com.openapi.domain.dto.resonse.AgentListResponse;
import com.openapi.domain.dto.resonse.AgentResponse;
import com.openapi.service.AgentService;
import com.openapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 13225
 * @date 2025/9/29 12:00
 */

@Slf4j
@CrossOrigin(origins = "*") // 跨域
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;
    private final UserService userService;

    // 创建Agent
    @PostMapping("/create")
    public BaseResponse<AgentResponse> createAgent(
            @RequestParam("avatar") MultipartFile avatar,
            @RequestParam("userId") String userId,
            @RequestParam("name") String name,
            @RequestParam("description") String description
    ) {
        // 参数校验
        if (!StringUtils.hasText(name)){
//            throw new AppException(CommonExceptions.PARAM_ERROR); // 等价
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!StringUtils.hasText(description)){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!userService.checkUserExistById(userId)){
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }

        AgentAo agentAo = agentService.createAgent(avatar, userId, name, description);

        AgentResponse response = new AgentResponse();
        response.setAgentAo(agentAo);

        return BaseResponse.getResponseEntitySuccess(response);
    }

    // 获取AgentInfo
    @GetMapping("/getInfo")
    public BaseResponse<AgentResponse> getAgentInfo(
            @RequestParam("agentId") String agentId
    ) {
        // 参数校验
        if (!StringUtils.hasText(agentId)){
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        AgentAo agentAo = agentService.getAgentById(agentId);
        AgentResponse response = new AgentResponse();
        response.setAgentAo(agentAo);
        return BaseResponse.getResponseEntitySuccess(response);
    }

    // 获取AgentList
    @GetMapping("/getList")
    public BaseResponse<AgentListResponse> getAgentList(
            @RequestParam("userId") String userId
    ){
        // 参数校验
        if (!userService.checkUserExistById(userId)){
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }

        List<AgentAo> agentAos = agentService.getUserAgentsAo(userId);
        AgentListResponse response = new AgentListResponse();
        response.setAgentAos(agentAos);
        return BaseResponse.getResponseEntitySuccess(response);
    }

    // 获取用户和Agent的最近聊天list
    @GetMapping("/getLastAgentChatList")
    public BaseResponse<AgentLastChatListResponse> getLastAgentChatList(
            @RequestParam("userId") String userId
    ){
        // 参数校验
        if (!userService.checkUserExistById(userId)){
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }

        List<AgentChatAo> agentChatAos = agentService.getLastAgentChatList(userId);

        AgentLastChatListResponse response = new AgentLastChatListResponse();
        response.setAgentChatAos(agentChatAos);

        return BaseResponse.getResponseEntitySuccess(response);
    }

}
