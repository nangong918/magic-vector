package com.openapi.controller;

import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.resonse.CreateAgentResponse;
import com.openapi.domain.vo.AgentVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
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

    // 创建Agent
    @PostMapping("/create")
    public BaseResponse<CreateAgentResponse> createAgent(
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

        CreateAgentResponse response = new CreateAgentResponse();
        response.setAgentId("1");
        AgentVo agentVo = new AgentVo();
        agentVo.setName(name);
        agentVo.setDescription(description);
        agentVo.setAvatarUrl("https://example.com/avatar.png");
        response.setAgentVo(agentVo);

        return BaseResponse.getResponseEntitySuccess(response);
    }

}
