package com.demo.component.interceptor;

import com.alibaba.fastjson.JSON;
import com.demo.config.AuthRouteProperties;
import com.demo.domain.constant.ExceptionEnums;
import com.demo.domain.constant.error.UserExceptions;
import com.demo.domain.dto.BaseResponse;
import com.demo.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final AuthTokenService authTokenService;
    private final AuthRouteProperties authRouteProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        String accessToken = request.getHeader(authRouteProperties.getAccessTokenHeader());
        String userIdStr = request.getHeader(authRouteProperties.getUserIdHeader());
        if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(userIdStr)) {
            writeError(response, UserExceptions.NO_TOKEN_FORBIDDEN_CALL_API);
            return false;
        }
        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            writeError(response, UserExceptions.USER_NOT_EXIST);
            return false;
        }
        if (!authTokenService.verifyAccessToken(userId, accessToken)) {
            writeError(response, UserExceptions.ACCESS_TOKEN_INVALID);
            return false;
        }
        if (isTouristRequest(userId, accessToken) && isTouristForbiddenPath(request.getRequestURI())) {
            writeError(response, UserExceptions.TOURIST_FORBIDDEN_CALL_API);
            return false;
        }
        return true;
    }

    private boolean isTouristRequest(long userId, String accessToken) {
        return userId == authRouteProperties.getTouristUserId()
                && StringUtils.hasText(accessToken)
                && accessToken.equals(authRouteProperties.getTouristAccessToken());
    }

    private boolean isTouristForbiddenPath(String path) {
        return authRouteProperties.getTouristForbiddenPaths()
                .stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void writeError(HttpServletResponse response, ExceptionEnums exceptionEnums) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(BaseResponse.LogBackError(exceptionEnums)));
    }
}
