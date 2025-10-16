package com.openapi.domain.constant.realtime;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/10/7 17:14
 */
@Getter
public enum RealtimeDataTypeEnum {
    // connect
    CONNECT("connect"),
    // disconnect
    DISCONNECT("disconnect"),
    // start_send/receive
    START("start"),
    // stop_send/receive
    STOP("stop"),
    // audio_chunk
    AUDIO_CHUNK("audio_chunk"),
    // text_message
    TEXT_MESSAGE("text_message"),
    ;

    public static final String TYPE = "type";
    public static final String DATA = "data";

    private final String type;

    RealtimeDataTypeEnum(String type) {
        this.type = type;
    }

    // type -> enum
    @NotNull
    public static RealtimeDataTypeEnum getByType(String type) {
        for (RealtimeDataTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return TEXT_MESSAGE;
    }
}
