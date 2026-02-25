package com.data.domain.constant.chat

enum class MessageTypeEnum(val value: Int, val desc: String) {
    UNKNOWN(-1, "unknown"),
    TEXT(0, "text"),
    IMAGE(1, "image");

    companion object {
        fun getMessageType(value: Int): MessageTypeEnum {
            return MessageTypeEnum.entries.find { it.value == value } ?: UNKNOWN
        }
    }
}