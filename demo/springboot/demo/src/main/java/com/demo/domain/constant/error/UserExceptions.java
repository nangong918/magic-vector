package com.demo.domain.constant.error;

import com.demo.domain.constant.ExceptionEnums;
import lombok.Getter;

@Getter
public enum UserExceptions implements ExceptionEnums {
    USER_NOT_EXIST("U_10001", "用户不存在"),
    ACCOUNT_ALREADY_EXIST("U_10002", "账号已存在"),
    ACCOUNT_OR_PASSWORD_ERROR("U_10003", "账号或密码错误"),
    ACCESS_TOKEN_INVALID("U_10004", "access_token无效"),
    NO_TOKEN_FORBIDDEN_CALL_API("U_10005", "无Token禁止调用API"),
    TOURIST_FORBIDDEN_CALL_API("U_10006", "游客账号无权限访问该接口"),
    ;

    private final String code;
    private final String message;

    UserExceptions(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
