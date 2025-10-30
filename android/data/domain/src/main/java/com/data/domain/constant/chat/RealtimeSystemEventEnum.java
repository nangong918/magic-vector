package com.data.domain.constant.chat;


import androidx.annotation.NonNull;

/**
 * @author 13225
 * @date 2025/10/29 14:32
 */
public enum RealtimeSystemEventEnum {
    // null
    NULL("null", "NULL"),
    /**
     * 上传照片
     * @see com.data.domain.dto.ws.reponse.SystemTextResponse
     */
    UPLOAD_PHOTO("vision.upload.photo", "上传照片"),
    ;

    private final String code;
    private final String message;
    RealtimeSystemEventEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
    public String getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }

    @NonNull
    public static RealtimeSystemEventEnum getByCode(String code) {
        for (RealtimeSystemEventEnum value : RealtimeSystemEventEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return NULL;
    }
}
