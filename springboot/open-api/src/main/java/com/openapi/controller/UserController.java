package com.openapi.controller;

import com.openapi.converter.UserConverter;
import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.constant.error.UserExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.request.UserLoginRequest;
import com.openapi.domain.dto.request.UserPasswordUpdateRequest;
import com.openapi.domain.dto.request.UserTokenVerifyRequest;
import com.openapi.domain.dto.resonse.UserAuthResponse;
import com.openapi.domain.dto.resonse.UserPasswordUpdateResponse;
import com.openapi.domain.dto.resonse.UserTokenVerifyResponse;
import com.openapi.domain.module.user.UserModule;
import jakarta.validation.Valid;
import com.openapi.service.AuthTokenService;
import com.openapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final UserConverter userConverter;

    @PostMapping("/register")
    public BaseResponse<UserAuthResponse> register(
            @RequestParam("account") String account,
            @RequestParam("password") String password,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        // FormData 场景不使用 @Valid/@RequestBody，统一手动校验
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (userService.checkUserExistByAccount(account)) {
            return BaseResponse.LogBackError(UserExceptions.ACCOUNT_ALREADY_EXIST);
        }

        String registerName = StringUtils.hasText(name) ? name : account;
        if (avatar != null) {
            log.info("[register] avatar upload is skipped temporarily. account: {}", account);
        }
        Long userId = userService.createUser(
                avatar,
                registerName,
                account,
                password
        );
        if (userId == null) {
            return BaseResponse.LogBackError(CommonExceptions.SYSTEM_ERROR);
        }

        UserModule userModule = userService.getUserModuleById(userId);
        if (userModule == null) {
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }
        String accessToken = authTokenService.issueAccessToken(userModule.getUserId());
        return BaseResponse.getResponseEntitySuccess(userConverter.moduleToAuthResponse(userModule, accessToken, ""));
    }

    @PostMapping("/login")
    public BaseResponse<UserAuthResponse> login(
            @Valid @RequestBody UserLoginRequest request
    ) {
        String account = request.getAccount();
        String password = request.getPassword();
        if (!userService.checkPassword(account, password)) {
            return BaseResponse.LogBackError(UserExceptions.ACCOUNT_OR_PASSWORD_ERROR);
        }

        UserModule userModule = userService.getUserModuleByAccount(account);
        if (userModule == null || userModule.getUserId() == null) {
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }
        String accessToken = authTokenService.issueAccessToken(userModule.getUserId());
        return BaseResponse.getResponseEntitySuccess(userConverter.moduleToAuthResponse(userModule, accessToken, ""));
    }

    @PostMapping("/token/verify")
    public BaseResponse<UserTokenVerifyResponse> verifyAccessToken(
            @Valid @RequestBody UserTokenVerifyRequest request
    ) {
        String accessToken = request.getAccessToken();
        Long userId = request.getUserId();
        UserTokenVerifyResponse response = new UserTokenVerifyResponse();
        response.setUserId(userId);
        if (!authTokenService.verifyAccessToken(userId, accessToken)) {
            response.setValid(false);
            response.setMessage(UserExceptions.ACCESS_TOKEN_INVALID.getMessage());
            return BaseResponse.getResponseEntitySuccess(response);
        }
        response.setValid(true);
        response.setMessage("ok");
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/password/update")
    public BaseResponse<UserPasswordUpdateResponse> updatePassword(
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        UserPasswordUpdateResponse response = new UserPasswordUpdateResponse();
        Long userId;
        try {
            userId = Long.parseLong(request.getUserId());
        } catch (Exception e) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        response.setUserId(userId);
        boolean updated = userService.updatePasswordById(userId, request.getOldPassword(), request.getNewPassword());
        response.setUpdated(updated);
        response.setMessage(updated ? "ok" : "旧密码错误或用户不存在");
        return BaseResponse.getResponseEntitySuccess(response);
    }
}
