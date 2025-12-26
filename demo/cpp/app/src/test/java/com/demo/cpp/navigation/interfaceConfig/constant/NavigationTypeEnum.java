package com.demo.cpp.navigation.interfaceConfig.constant;


import androidx.annotation.NonNull;

/**
 * <p>
 * 导航栏类型枚举类
 * </p>
 * 屏幕管理:0，音频管理:1，环境控制:2，预监回显:3，监控中心:4，设备管理:5，拼接设置:6，输入配置:7，高级设置:8
 * @author Lemon
 * @since 2025-12-25 17:10:25
 */
public enum NavigationTypeEnum {
    SCREEN_MANAGEMENT(0, "屏幕管理"),
    AUDIO_MANAGEMENT(1, "音频管理"),
    ENVIRONMENT_CONTROL(2, "环境控制"),
    PREVIEW_ECHO(3, "预监回显"),
    MONITOR_CENTER(4, "监控中心"),
    DEVICE_MANAGEMENT(5, "设备管理"),
    CONNECTION_SETTING(6, "拼接设置"),
    INPUT_CONFIGURATION(7, "输入配置"),
    ADVANCED_SETTING(8, "高级设置")
    ;

    private final Integer code;
    private final String name;
    NavigationTypeEnum(Integer code, String name) {
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
    public static NavigationTypeEnum getNameByCode(Integer code) {
        for (NavigationTypeEnum navigationTypeEnum : NavigationTypeEnum.values()) {
            if (navigationTypeEnum.getCode().equals(code)) {
                return navigationTypeEnum;
            }
        }
        return SCREEN_MANAGEMENT;
    }
}
