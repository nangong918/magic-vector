package com.openapi.controller;

import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.constant.error.UserExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.request.UserLoginRequest;
import com.openapi.domain.dto.request.UserTokenVerifyRequest;
import com.openapi.domain.dto.resonse.UserAuthResponse;
import com.openapi.domain.dto.resonse.UserTokenVerifyResponse;
import com.openapi.domain.module.user.UserModule;
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

    @PostMapping("/register")
    public BaseResponse<UserAuthResponse> register(
            @RequestParam("account") String account,
            @RequestParam("password") String password,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (userService.checkUserExistByAccount(account)) {
            return BaseResponse.LogBackError(UserExceptions.ACCOUNT_ALREADY_EXIST);
        }

        String registerName = StringUtils.hasText(name) ? name : account;

        // TODO minio 尚未稳定配置时，注册先不处理头像文件，避免影响主流程。
        if (avatar != null) {
            log.info("[register] avatar upload is skipped temporarily. account: {}", account);
        }
        Long userId = userService.createUser(
                null,
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
        return BaseResponse.getResponseEntitySuccess(toAuthResponse(userModule, accessToken, ""));
    }

    @PostMapping("/login")
    public BaseResponse<UserAuthResponse> login(
            @RequestBody UserLoginRequest request
    ) {
        String account = request == null ? null : request.getAccount();
        String password = request == null ? null : request.getPassword();
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        if (!userService.checkPassword(account, password)) {
            return BaseResponse.LogBackError(UserExceptions.ACCOUNT_OR_PASSWORD_ERROR);
        }

        UserModule userModule = userService.getUserModuleByAccount(account);
        if (userModule == null || userModule.getUserId() == null) {
            return BaseResponse.LogBackError(UserExceptions.USER_NOT_EXIST);
        }
        String accessToken = authTokenService.issueAccessToken(userModule.getUserId());
        return BaseResponse.getResponseEntitySuccess(toAuthResponse(userModule, accessToken, ""));
    }

    @PostMapping("/token/verify")
    public BaseResponse<UserTokenVerifyResponse> verifyAccessToken(
            @RequestBody UserTokenVerifyRequest request
    ) {
        String accessToken = request == null ? null : request.getAccessToken();
        Long userId = request == null ? null : request.getUserId();
        UserTokenVerifyResponse response = new UserTokenVerifyResponse();
        response.setUserId(userId);
        if (userId == null || userId <= 0L || !StringUtils.hasText(accessToken)) {
            response.setValid(false);
            response.setMessage(CommonExceptions.PARAM_ERROR.getMessage());
            return BaseResponse.getResponseEntitySuccess(response);
        }
        if (!authTokenService.verifyAccessToken(userId, accessToken)) {
            response.setValid(false);
            response.setMessage(UserExceptions.ACCESS_TOKEN_INVALID.getMessage());
            return BaseResponse.getResponseEntitySuccess(response);
        }
        response.setValid(true);
        response.setMessage("ok");
        return BaseResponse.getResponseEntitySuccess(response);
    }

    private UserAuthResponse toAuthResponse(UserModule userModule, String accessToken, String avatarUrl) {
        UserAuthResponse response = new UserAuthResponse();
        response.setUserId(userModule.getUserId());
        response.setAccount(userModule.getAccount());
        response.setName(userModule.getName());
        response.setAvatarUrl(avatarUrl);
        response.setAccessToken(accessToken);
        return response;
    }
}
