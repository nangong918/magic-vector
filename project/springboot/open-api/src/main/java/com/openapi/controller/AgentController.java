package com.openapi.controller;

import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.ao.AgentChatAo;
import com.openapi.converter.AgentHttpConverter;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.constant.error.UserExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.http.request.AgentDeleteRequest;
import com.openapi.domain.dto.http.resonse.AgentChatDto;
import com.openapi.domain.dto.http.resonse.AgentLastChatListResponse;
import com.openapi.domain.dto.http.resonse.AgentListResponse;
import com.openapi.domain.dto.http.resonse.AgentResponse;
import com.openapi.service.AgentService;
import com.openapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final AgentHttpConverter agentHttpConverter;

    // 创建Agent
    @PostMapping("/create")
    public BaseResponse<AgentResponse> createAgent(
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
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
        Long userIdLong = parseLong(userId);
        if (userIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!userService.checkUserExistById(userIdLong)){
            log.warn("用户:{} 不存在", userId);
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }

        AgentAo agentAo = agentService.createAgent(avatar, userIdLong, name, description);

        AgentResponse response = new AgentResponse();
        response.setAgent(agentHttpConverter.aoToDto(agentAo));

        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/update")
    public BaseResponse<AgentResponse> updateAgent(
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam("agentId") String agentId,
            @RequestParam("userId") String userId,
            @RequestParam("name") String name,
            @RequestParam("description") String description
    ) {
        if (!StringUtils.hasText(agentId) || !StringUtils.hasText(userId)
                || !StringUtils.hasText(name) || !StringUtils.hasText(description)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        Long agentIdLong = parseLong(agentId);
        Long userIdLong = parseLong(userId);
        if (agentIdLong == null || userIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        AgentAo agentAo = agentService.updateAgent(avatar, agentIdLong, userIdLong, name, description);
        if (agentAo == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        AgentResponse response = new AgentResponse();
        response.setAgent(agentHttpConverter.aoToDto(agentAo));
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/delete")
    public BaseResponse<AgentResponse> deleteAgent(
            @Valid @RequestBody AgentDeleteRequest request
    ) {
        Long agentIdLong = parseLong(request.getAgentId());
        Long userIdLong = parseLong(request.getUserId());
        if (agentIdLong == null || userIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        boolean deleted = agentService.deleteAgent(agentIdLong, userIdLong);
        if (!deleted) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        AgentResponse response = new AgentResponse();
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
        Long agentIdLong = parseLong(agentId);
        if (agentIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        AgentAo agentAo = agentService.getAgentById(agentIdLong);
        AgentResponse response = new AgentResponse();
        response.setAgent(agentHttpConverter.aoToDto(agentAo));
        return BaseResponse.getResponseEntitySuccess(response);
    }

    // 获取AgentList
    @GetMapping("/getList")
    public BaseResponse<AgentListResponse> getAgentList(
            @RequestParam("userId") String userId
    ){
        // 参数校验
        log.info("getAgentList userId: {}", userId);
        Long userIdLong = parseLong(userId);
        log.info("getAgentList userIdLong: {}", userIdLong);
        if (userIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!userService.checkUserExistById(userIdLong)){
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }

        List<AgentChatAo> agentChatAos = agentService.getLastAgentChatList(userIdLong);
        AgentListResponse response = new AgentListResponse();
        response.setAgentList(agentChatAos.stream().map(agentHttpConverter::chatAoToDto).toList());
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @GetMapping("/getListFull")
    public BaseResponse<AgentListResponse> getAgentListFull(
            @RequestParam("userId") String userId
    ){
        return getAgentList(userId);
    }

    @GetMapping("/getListPage")
    public BaseResponse<AgentListResponse> getAgentListPage(
            @RequestParam("userId") String userId,
            @RequestParam("sortField") String sortField,
            @RequestParam("sortOrder") String sortOrder,
            @RequestParam("pageDirection") String pageDirection,
            @RequestParam("cursor") String cursor,
            @RequestParam("limit") Integer limit
    ) {
        if (!StringUtils.hasText(sortField) || !StringUtils.hasText(sortOrder)
                || !StringUtils.hasText(pageDirection) || limit == null || limit <= 0) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        Long userIdLong = parseLong(userId);
        if (userIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!userService.checkUserExistById(userIdLong)){
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }

        List<AgentChatDto> allAgents = new ArrayList<>(
                agentService.getLastAgentChatList(userIdLong).stream().map(agentHttpConverter::chatAoToDto).toList()
        );
        Comparator<AgentChatDto> comparator = buildAgentComparator(sortField);
        if (comparator == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        allAgents.sort(comparator);
        if ("DESC".equalsIgnoreCase(sortOrder)) {
            allAgents.sort(comparator.reversed());
        } else if (!"ASC".equalsIgnoreCase(sortOrder)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }

        List<AgentChatDto> filteredAgents = applyCursor(allAgents, sortField, pageDirection, cursor);
        int end = Math.min(limit, filteredAgents.size());
        List<AgentChatDto> pagedAgents = end > 0 ? filteredAgents.subList(0, end) : List.of();

        AgentListResponse response = new AgentListResponse();
        response.setAgentList(pagedAgents);
        return BaseResponse.getResponseEntitySuccess(response);
    }

    // 获取用户和Agent的最近聊天list
    @GetMapping("/getLastAgentChatList")
    public BaseResponse<AgentLastChatListResponse> getLastAgentChatList(
            @RequestParam("userId") String userId
    ){
        // 参数校验
        log.info("getLastAgentChatList userId: {}", userId);
        Long userIdLong = parseLong(userId);
        log.info("getLastAgentChatList userIdLong: {}", userIdLong);
        if (userIdLong == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!userService.checkUserExistById(userIdLong)){
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }

        List<AgentChatAo> agentChatAos = agentService.getLastAgentChatList(userIdLong);

        AgentLastChatListResponse response = new AgentLastChatListResponse();
        response.setAgentChatAos(agentChatAos);

        return BaseResponse.getResponseEntitySuccess(response);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            log.error("参数转换错误", e);
            return null;
        }
    }

    private Comparator<AgentChatDto> buildAgentComparator(String sortField) {
        if ("agentId".equalsIgnoreCase(sortField)) {
            return Comparator.comparing(agent -> parseLong(agent.getAgentId()), Comparator.nullsLast(Long::compareTo));
        }
        if ("name".equalsIgnoreCase(sortField)) {
            return Comparator.comparing(AgentChatDto::getName, Comparator.nullsLast(String::compareTo));
        }
        if ("lastChatTime".equalsIgnoreCase(sortField)) {
            return Comparator.comparing(agent -> parseLong(agent.getLastChatTime()), Comparator.nullsLast(Long::compareTo));
        }
        return null;
    }

    private List<AgentChatDto> applyCursor(
            List<AgentChatDto> agents,
            String sortField,
            String pageDirection,
            String cursor
    ) {
        if (!StringUtils.hasText(cursor)) {
            return agents;
        }
        if ("after".equalsIgnoreCase(pageDirection)) {
            return agents.stream().filter(agent -> compareByCursor(agent, sortField, cursor) > 0).toList();
        }
        if ("before".equalsIgnoreCase(pageDirection)) {
            return agents.stream().filter(agent -> compareByCursor(agent, sortField, cursor) < 0).toList();
        }
        return List.of();
    }

    private int compareByCursor(AgentChatDto agent, String sortField, String cursor) {
        if ("agentId".equalsIgnoreCase(sortField) || "lastChatTime".equalsIgnoreCase(sortField)) {
            Long left = "agentId".equalsIgnoreCase(sortField) ? parseLong(agent.getAgentId()) : parseLong(agent.getLastChatTime());
            Long right = parseLong(cursor);
            if (left == null || right == null) {
                return 0;
            }
            return left.compareTo(right);
        }
        if ("name".equalsIgnoreCase(sortField)) {
            String left = agent.getName();
            if (left == null) {
                return -1;
            }
            return left.compareTo(cursor);
        }
        return 0;
    }

}
