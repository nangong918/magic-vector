package com.data.domain.constant.chat;

import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/10/7 17:14
 */
public enum RealtimeRequestDataTypeEnum {
    /**
     * connect
     * @see com.data.domain.dto.ws.RealtimeChatConnectRequest
     */
    CONNECT("connect"),
//    // disconnect
//    DISCONNECT("disconnect"),
    /**
     * start_send
     * String: flag
     */
    START_AUDIO_RECORD("start_audio_record"),
    /**
     * stop_send
     * String: flag
     */
    STOP_AUDIO_RECORD("stop_audio_record"),
    /**
     * audio_chunk
     * String: base64Audio
     */
    AUDIO_CHUNK("audio_chunk"),
    /**
     * user_text_message
     * String: userQuestion
     */
    USER_TEXT_MESSAGE("user_text_message"),
    ;

    public static final String TYPE = "type";
    public static final String DATA = "data";

    private final String type;

    public String getType() {
        return type;
    }

    RealtimeRequestDataTypeEnum(String type) {
        this.type = type;
    }

    // type -> enum
    @NotNull
    public static RealtimeRequestDataTypeEnum getByType(String type) {
        for (RealtimeRequestDataTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return USER_TEXT_MESSAGE;
    }
}
