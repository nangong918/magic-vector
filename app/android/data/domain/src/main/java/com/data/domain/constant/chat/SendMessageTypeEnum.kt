package com.data.domain.constant.chat

/**
 * 发送消息类型枚举
 * @see RoleTypeEnum
 */
enum class SendMessageTypeEnum(val value: Int, val desc: String) {

    VIEW_TYPE_AGENT(0, "agent"),
    VIEW_TYPE_USER(1, "user");

    companion object {
        fun getSendMessageType(value: Int): SendMessageTypeEnum {
            return SendMessageTypeEnum.entries.find { it.value == value } ?: VIEW_TYPE_USER
        }
    }

}