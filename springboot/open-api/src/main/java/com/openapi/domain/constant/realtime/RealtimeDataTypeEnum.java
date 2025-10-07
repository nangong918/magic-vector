package com.openapi.domain.constant.realtime;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/10/7 17:14
 */
@Getter
public enum RealtimeDataTypeEnum {
    // start_recording
    START_RECORDING("start_recording"),
    // stop_recording
    STOP_RECORDING("stop_recording"),
    // audio_chunk
    AUDIO_CHUNK("audio_chunk"),
    // text_message
    TEXT_MESSAGE("text_message"),
    ;

    public static final String TYPE = "type";

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
