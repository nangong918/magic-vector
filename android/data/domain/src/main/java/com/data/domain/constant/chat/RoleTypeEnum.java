package com.data.domain.constant.chat;

import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/10/16 10:58
 * @see SendMessageTypeEnum
 */
public enum RoleTypeEnum {
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    AGENT(0),
    // 接收方: 0: user, 1: agent (相当于isUser ? 1 : 0)
    USER(1);
    private final int value;
    RoleTypeEnum(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
    @NotNull
    public static RoleTypeEnum getByValue(int value) {
        for (RoleTypeEnum roleTypeEnum : RoleTypeEnum.values()) {
            if (roleTypeEnum.value == value) {
                return roleTypeEnum;
            }
        }
        return AGENT;
    }
    public static boolean isUser(int value) {
        return value == USER.value;
    }
}
