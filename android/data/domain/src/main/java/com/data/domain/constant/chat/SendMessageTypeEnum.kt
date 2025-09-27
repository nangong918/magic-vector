package com.data.domain.constant.chat

enum class SendMessageTypeEnum(val value: Int, val desc: String) {

    VIEW_TYPE_SENDER(0, "发送者"),
    VIEW_TYPE_RECEIVER(1, "接收者");

    companion object {
        fun getSendMessageType(value: Int): SendMessageTypeEnum {
            return SendMessageTypeEnum.entries.find { it.value == value } ?: VIEW_TYPE_SENDER
        }
    }

}