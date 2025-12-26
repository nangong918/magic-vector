package com.demo.cpp.navigation.interfaceConfig.constant;


import androidx.annotation.NonNull;

/**
 * <p>
 * 导航栏开关类型枚举类
 * </p>
 * 开关：0: 关闭 1: 开启 2: 不可关闭
 * @author Lemon
 * @since 2025-12-25 17:15:09
 */
public enum NavigationSwitchEnum {
    CLOSE(0, "关闭"),
    OPEN(1, "开启"),
    NOT_CLOSE(2, "不可关闭");

    private final Integer code;
    private final String name;

    NavigationSwitchEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @NonNull
    public static NavigationSwitchEnum getNameByCode(Integer code) {
        for (NavigationSwitchEnum navigationSwitchEnum : NavigationSwitchEnum.values()) {
            if (navigationSwitchEnum.getCode().equals(code)) {
                return navigationSwitchEnum;
            }
        }
        return CLOSE;
    }
}
