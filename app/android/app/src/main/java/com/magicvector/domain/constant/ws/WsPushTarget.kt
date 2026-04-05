package com.magicvector.domain.constant.ws

/**
 * 服务端下行 [WsEvent.CONTROL_COMMAND_SB] 等消息时的推送目标。
 */
enum class WsPushTarget(val value: String) {
    /** 发往 Android App 客户端 */
    ANDROID("android"),

    /** 发往 RK 设备客户端 */
    RK("rk")
}
