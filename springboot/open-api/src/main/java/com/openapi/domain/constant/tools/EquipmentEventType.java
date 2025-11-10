package com.openapi.domain.constant.tools;

/**
 * @author 13225
 * @date 2025/11/10 11:54
 */
public enum EquipmentEventType {
    // none
    NONE("none"),
    // 移动事件 motion
    MOTION("motion"),
    // 表情事件 emoji
    EMOJI("emoji")
    ;

    public final String code;

    EquipmentEventType(String code) {
        this.code = code;
    }

    public static EquipmentEventType getByCode(String code) {
        for (EquipmentEventType value : EquipmentEventType.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return NONE;
    }
}
