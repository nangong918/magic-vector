package com.openapi.domain.ao.realtimeChat;

import com.openapi.domain.constant.tools.AICallEnum;
import lombok.Data;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 13225
 * @date 2025/11/10 9:49
 * 表示各个Mcp是否可以调用的状态
 * 暂时不使用枚举
 */
@Data
public class McpSwitch {
    public String equipment = McpEquipment.PHONE.code;
    public String camera = McpSwitchMode.CLOSE.code;
    public String motion = McpSwitchMode.CLOSE.code;
    public String emojiAndMood = McpSwitchMode.CLOSE.code;


    public enum McpSwitchMode implements AICallEnum {
        // close
        CLOSE("close", "关闭"),
        // freely
        FREELY("freely", "Agent自由调度"),
        // commands
        COMMANDS("commands", "用户有相关指令再调用"),
        ;

        public final String code;
        public final String name;

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getName() {
            return name;
        }

        McpSwitchMode(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public static McpSwitchMode getByCode(String code) {
            for (McpSwitchMode value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return CLOSE;
        }

        public static void main(String[] args) {
            System.out.println(AICallEnum.getAIDocs(McpSwitchMode.class));
        }
    }

    public enum McpEquipment implements AICallEnum{
        // phone
        PHONE("phone", "手机App"),
        // device
        DEVICE("device", "嵌入式设备"),
        ;

        public final String code;
        public final String name;

        @Override
        public String getCode() {
            return code;
        }

        @Override
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

        public static void main(String[] args) {
            System.out.println(AICallEnum.getAIDocs(McpEquipment.class));
        }

    }

    public String getAICallInstructions(){
        Map<String, String> map = new HashMap<>();
        map.put("当前设备", McpEquipment.getByCode(equipment).getName());
        map.put("相机调用权限", McpSwitchMode.getByCode(camera).getName());
        map.put("运动调用权限", McpSwitchMode.getByCode(motion).getName());
        map.put("表情和心情调用权限", McpSwitchMode.getByCode(emojiAndMood).getName());

        // map -> jsonString
        return map.toString();
    }

    public static void main(String[] args) {
        McpSwitch mcpSwitch = new McpSwitch();
        System.out.println(mcpSwitch.getAICallInstructions());
    }
}
