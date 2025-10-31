package com.openapi.domain.constant.realtime;


import lombok.NonNull;

/**
 * @author 13225
 * @date 2025/10/29 14:32
 */
public enum RealtimeSystemRequestEventEnum {
    // null
    NULL("null", "NULL"),
    /**
     * 上传照片
     * @see com.openapi.domain.dto.ws.request.UploadPhotoRequest
     */
    UPLOAD_PHOTO("vision.upload.photo", "上传照片"),
    ;

    public static final String EVENT_KET = "event";
    private final String code;
    private final String message;
    RealtimeSystemRequestEventEnum(String code, String message) {
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
    public static RealtimeSystemRequestEventEnum getByCode(String code) {
        for (RealtimeSystemRequestEventEnum value : RealtimeSystemRequestEventEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return NULL;
    }
}
