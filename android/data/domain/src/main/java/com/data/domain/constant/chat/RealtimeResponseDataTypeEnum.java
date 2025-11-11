package com.data.domain.constant.chat;

import com.data.domain.ao.mixLLM.MixLLMEvent;
import com.data.domain.dto.ws.reponse.RealtimeChatTextResponse;
import com.data.domain.dto.ws.reponse.SystemTextResponse;

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
     * 事件列表: List<MixLLMEvent>
     * @see MixLLMEvent
     */
    EVENT_LIST("event_list"),
    /**
     * text_chat_response（user response、fragment回复）
     * @see RealtimeChatTextResponse
     */
    TEXT_CHAT_RESPONSE("text_chat_response"),
    /**
     * text_system_response
     * @see SystemTextResponse
     */
    TEXT_SYSTEM_RESPONSE("text_system_response"),
    /**
     * whole_chat_response 整句回复
     * @see RealtimeChatTextResponse
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
