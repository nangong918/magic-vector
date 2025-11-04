package com.openapi.domain.constant.error;

import com.openapi.domain.constant.ExceptionEnums;
import lombok.Getter;

/**
 * @author 13225
 * @date 2025/6/26 17:22
 */
@Getter
public enum AgentExceptions implements ExceptionEnums {

    // Agent不存在
    AGENT_NOT_EXIST("A_10001", "Agent不存在"),
    // 对话不能为null
    CHAT_CAN_NOT_BE_NULL("A_10002", "对话不能为null"),
    ;

    private final String code;
    private final String message;

    AgentExceptions(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // code -> o
    public static AgentExceptions getByCode(String code) {
        for (AgentExceptions value : AgentExceptions.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
