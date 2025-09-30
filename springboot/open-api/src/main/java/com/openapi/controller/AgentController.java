package com.openapi.controller;

import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.resonse.AgentResponse;
import com.openapi.service.AgentService;
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

    // 创建Agent
    @PostMapping("/create")
    public BaseResponse<AgentResponse> createAgent(
            @RequestParam("avatar") MultipartFile avatar,
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

        AgentAo agentAo = agentService.createAgent(avatar, name, description);

        AgentResponse response = new AgentResponse();
        response.setAgentAo(agentAo);

        return BaseResponse.getResponseEntitySuccess(response);
    }

    // 获取AgentAo
    @GetMapping("/getInfo")
    public BaseResponse<AgentResponse> getAgentAo(
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

}
