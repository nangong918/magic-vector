package com.openapi.domain.constant.ws;

import lombok.Getter;

/**
 * 服务端下行 {@link WsEvent#CONTROL_COMMAND_SB} 等消息时的推送目标。
 */
public enum WsPushTarget {

    /** 发往 Android App 客户端 */
    ANDROID("android"),

    /** 发往 RK 设备客户端 */
    RK("rk");

    @Getter
    private final String value;

    WsPushTarget(String value) {
        this.value = value;
    }
}
