package com.openapi.domain.constant.tools;

import lombok.NonNull;

/**
 * @author 13225
 * @date 2025/11/10 11:15
 */
public enum EmojiEvent implements AICallEnum {
    /// 状态值
    // 无表情、复位
    NONE("none", "无表情、复位"),

    /// 心情 (心情和表情是可以叠加的)
    // 普通心情
    EMO_NORMAL("emo.normal", "普通心情"),
    // 好心情 -> 笑
    EMO_GOOD("emo.good", "好心情"),
    // 差心情 -> 哭泣
    EMO_BAD("emo.bad", "差心情"),

    /// 表情
    // 眨眼
    EYE_BLINK("eye.blink", "眨眼"),
    // 眯眼睛
    EYE_WINK("eye.wink", "眯眼睛"),
    // 闭眼
    EYE_CLOSE("eye.close", "闭眼"),
    // 睁眼
    EYE_OPEN("eye.open", "睁眼"),
    // 疑惑
    EYE_CONFUSED("eye.confused", "疑惑"),
    // 思考中
    EYE_THINKING("eye.thinking", "思考中"),
    // 挣扎
    EYE_STRETCHING("eye.stretching", "挣扎"),
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

    EmojiEvent(String code, String name){
        this.code = code;
        this.name = name;
    }

    @NonNull
    public static EmojiEvent getByCode(String code){
        for (EmojiEvent value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return NONE;
    }

    public static void main(String[] args) {
        System.out.println(AICallEnum.getAIDocs(EmojiEvent.class));
    }
}
