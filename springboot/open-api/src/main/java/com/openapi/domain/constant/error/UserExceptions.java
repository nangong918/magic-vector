package com.openapi.domain.constant.error;

import com.openapi.domain.constant.ExceptionEnums;
import lombok.Getter;

/**
 * @author 13225
 * @date 2025/6/26 17:22
 */
@Getter
public enum UserExceptions implements ExceptionEnums {

    // 用户不存在
    USER_NOT_EXIST("U_10001", "用户不存在"),
    ;

    private final String code;
    private final String message;

    UserExceptions(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // code -> o
    public static UserExceptions getByCode(String code) {
        for (UserExceptions value : UserExceptions.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
