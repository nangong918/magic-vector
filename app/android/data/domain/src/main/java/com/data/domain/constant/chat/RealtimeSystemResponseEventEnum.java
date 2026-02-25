package com.data.domain.constant.chat;


import androidx.annotation.NonNull;

/**
 * @author 13225
 * @date 2025/10/29 14:32
 */
public enum RealtimeSystemResponseEventEnum {
    // null
    NULL("null", "NULL"),
    /**
     * 上传照片
     * @see com.data.domain.dto.ws.reponse.SystemTextResponse
     */
    UPLOAD_PHOTO("vision.upload.photo", "上传照片"),
    ;

    public static final String EVENT_KET = "event";
    private final String code;
    private final String message;
    RealtimeSystemResponseEventEnum(String code, String message) {
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
    public static RealtimeSystemResponseEventEnum getByCode(String code) {
        for (RealtimeSystemResponseEventEnum value : RealtimeSystemResponseEventEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return NULL;
    }
}
