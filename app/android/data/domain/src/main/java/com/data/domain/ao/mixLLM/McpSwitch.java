package com.data.domain.ao.mixLLM;

import androidx.annotation.NonNull;

/**
 * @author 13225
 * @date 2025/11/10 9:49
 * 表示各个Mcp是否可以调用的状态
 * 暂时不使用枚举
 */
public class McpSwitch {
    public String equipment = McpEquipment.PHONE.code;
    public String camera = McpSwitchMode.CLOSE.code;
    public String motion = McpSwitchMode.CLOSE.code;
    public String emojiAndMood = McpSwitchMode.CLOSE.code;


    public enum McpSwitchMode {
        // close
        CLOSE("close", "关闭"),
        // freely
        FREELY("freely", "Agent自由调度"),
        // commands
        COMMANDS("commands", "用户有相关指令再调用"),
        ;

        public final String code;
        public final String name;

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        McpSwitchMode(String code, String name) {
            this.code = code;
            this.name = name;
        }

        @NonNull
        public static McpSwitchMode getByCode(String code) {
            for (McpSwitchMode value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return CLOSE;
        }
    }

    public enum McpEquipment {
        // phone
        PHONE("phone", "手机App"),
        // device
        DEVICE("device", "嵌入式设备"),
        ;

        public final String code;
        public final String name;

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        McpEquipment(String code, String name) {
            this.code = code;
            this.name = name;
        }

        @NonNull
        public static McpEquipment getByCode(String code) {
            for (McpEquipment value : values()) {
                if (value.code.equals(code)){
                    return value;
                }
            }
            return PHONE;
        }

    }
}
