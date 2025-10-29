package com.data.domain.constant.chat;

import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/10/7 17:14
 */
public enum RealtimeResponseDataTypeEnum {
    /**
     * start tts
     * String: flag
     */
    START_TTS("start_tts"),
    /**
     * stop tts
     * String: flag
     */
    STOP_TTS("stop_tts"),
    /**
     * audio_chunk
     * String: base64Audio
     */
    AUDIO_CHUNK("audio_chunk"),
    /**
     * text_chat_response（user response、fragment回复）
     * @see com.data.domain.dto.ws.RealtimeChatTextResponse
     */
    TEXT_CHAT_RESPONSE("text_chat_response"),
    /**
     * text_system_response
     * @see com.data.domain.dto.ws.SystemTextResponse
     */
    TEXT_SYSTEM_RESPONSE("text_system_response"),
    /**
     * whole_chat_response 整句回复
     * @see com.data.domain.dto.ws.RealtimeChatTextResponse
     */
    WHOLE_CHAT_RESPONSE("whole_chat_response"),
    ;

    public static final String TYPE = "type";
    public static final String DATA = "data";

    private final String type;

    public String getType() {
        return type;
    }

    RealtimeResponseDataTypeEnum(String type) {
        this.type = type;
    }

    // type -> enum
    @NotNull
    public static RealtimeResponseDataTypeEnum getByType(String type) {
        for (RealtimeResponseDataTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return TEXT_CHAT_RESPONSE;
    }
}
