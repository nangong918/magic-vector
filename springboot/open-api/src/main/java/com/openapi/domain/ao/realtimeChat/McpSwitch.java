package com.openapi.domain.ao.realtimeChat;

import lombok.Data;

/**
 * @author 13225
 * @date 2025/11/10 9:49
 * 表示各个Mcp是否可以调用的状态
 * 暂时不使用枚举
 */
@Data
public class McpSwitch {
    public String camera = McpCamera.CLOSE.code;
    public String motion = McpMotion.CLOSE.code;
    public String emoji = McpEmoji.CLOSE.code;


    public enum McpCamera {
        // close
        CLOSE("close"),
        // 拍照模式：ShootingModel
        SHOOTING_MODEL("shootingModel"),
        // 录像模式：RecordingModel
        RECORDING_MODEL("recordingModel"),
        ;

        public final String code;

        McpCamera(String code) {
            this.code = code;
        }

        public static McpCamera getByCode(String code) {
            for (McpCamera value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return CLOSE;
        }
    }

    public enum McpMotion {
        // close
        CLOSE("close"),
        // Agent自由移动：Agent moves freely
        AGENT_MOVES_FREELY("agentMovesFreely"),
        // 识别用户指令移动: Identify user commands
        IDENTIFY_USER_COMMANDS("identifyUserCommands"),
        ;

        public final String code;

        McpMotion(String code) {
            this.code = code;
        }

        public static McpMotion getByCode(String code) {
            for (McpMotion value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return CLOSE;
        }
    }

    public enum McpEmoji {
        // close
        CLOSE("close"),
        // Agent自由调配表情：Agent expression freely
        AGENT_EXPRESSION_FREELY("agentExpressionFreely"),
        // 识别用户指令表情: Identify user commands
        IDENTIFY_USER_COMMANDS("identifyUserCommands"),
        ;

        public final String code;

        McpEmoji(String code) {
            this.code = code;
        }

        public static McpEmoji getByCode(String code) {
            for (McpEmoji value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return CLOSE;
        }
    }
}
