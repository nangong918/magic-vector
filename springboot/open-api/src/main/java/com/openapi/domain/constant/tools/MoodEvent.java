package com.openapi.domain.constant.tools;

import lombok.NonNull;

/**
 * @author 13225
 * @date 2025/11/10 11:15
 */
public enum MoodEvent implements AICallEnum {
    /// 状态值
    //
    NONE("none", "复位"),

    /// 心情 (心情和表情是可以叠加的)
    // 普通心情
    EMO_NORMAL("emo.normal", "普通心情"),
    // 好心情 -> 笑
    EMO_GOOD("emo.good", "好心情"),
    // 差心情 -> 哭泣
    EMO_BAD("emo.bad", "差心情"),
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

    MoodEvent(String code, String name){
        this.code = code;
        this.name = name;
    }

    @NonNull
    public static MoodEvent getByCode(String code){
        for (MoodEvent value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return NONE;
    }

    public static void main(String[] args) {
        System.out.println(AICallEnum.getAIDocs(MoodEvent.class));
    }
}
