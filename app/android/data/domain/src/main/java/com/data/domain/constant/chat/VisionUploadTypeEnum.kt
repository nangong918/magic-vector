package com.data.domain.constant.chat

enum class VisionUploadTypeEnum(val code: String) {
    UNKNOWN("unknown"),
    HTTP("http"),
    WS_FRAGMENT("ws-fragment"),
    RTMP("rtmp");

    companion object {
        fun getVisionUploadType(code: String): VisionUploadTypeEnum {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}