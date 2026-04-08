package com.openapi.domain.constant.ws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * {@code instruction_list} 列表项及 {@link com.openapi.domain.dto.ws.request.InstructionResultRequest} 中的类型判别，
 * 与 JSON 中的 {@code type} 字段对应（{@link #TTS} / {@link #MCP}）。
 */
public enum Instruction {

    /** 语音合成播放项 */
    TTS("tts"),

    /** MCP / 控制执行项 */
    MCP("mcp");

    @Getter
    private final String value;

    Instruction(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Instruction fromValue(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        for (Instruction v : values()) {
            if (v.value.equals(raw)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown Instruction: " + raw);
    }
}
