package com.openapi.controller;

import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.request.ControlCommandRequest;
import com.openapi.domain.dto.resonse.ControlCommandResponse;
import com.openapi.domain.dto.resonse.ControlStatusResponse;
import com.openapi.service.ControlConsoleService;
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

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/control")
public class ControlController {
    private final ControlConsoleService controlConsoleService;

    @GetMapping("/status")
    public BaseResponse<ControlStatusResponse> getControlStatus(
            @RequestParam("deviceId") String deviceId
    ) {
        if (!StringUtils.hasText(deviceId)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        ControlStatusResponse response = controlConsoleService.getControlStatus(deviceId);
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/command")
    public BaseResponse<ControlCommandResponse> sendControlCommand(
            @Valid @RequestBody ControlCommandRequest request
    ) {
        ControlCommandResponse response = controlConsoleService.dispatchCommand(request);
        return BaseResponse.getResponseEntitySuccess(response);
    }
}
