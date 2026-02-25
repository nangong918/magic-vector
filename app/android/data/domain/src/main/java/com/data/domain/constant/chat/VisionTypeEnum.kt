package com.data.domain.constant.chat

// image, image list, video
enum class VisionTypeEnum(val code: String) {
    UNKNOWN("null"),
    IMAGE("image"),
    IMAGE_LIST("image_list"),
    VIDEO("video"),
    ;

    companion object {
        fun getVisionType(code: String): VisionTypeEnum {
            return VisionTypeEnum.entries.find { it.code == code } ?: UNKNOWN
        }
    }
}