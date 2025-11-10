package com.openapi.domain.constant.tools;

import lombok.NonNull;

/**
 * @author 13225
 * @date 2025/11/10 11:36
 */
public enum MotionEvent implements AICallEnum {
    /// 状态值
    // 停止、复位
    STOP("stop", "停止"),

    /// 方向
    // 左转
    DIRECTION_LEFT("direction.left", "左转"),
    // 右转
    DIRECTION_RIGHT("direction.right", "右转"),

    /// 移动
    // 前进 move.forward
    MOVE_FORWARD("move.forward", "前进"),
    // 后退 move.backward
    MOVE_BACKWARD("move.backward", "后退"),

    /// 行为
    // 舞蹈 action.dance
    ACTION_DANCE("action.dance", "舞蹈"),
    // 起立 action.stand
    ACTION_STAND("action.stand", "起立"),
    // 趴下
    ACTION_SIT("action.sit", "趴下"),
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

    MotionEvent(String code, String name){
        this.code = code;
        this.name = name;
    }

    @NonNull
    public static MotionEvent getByCode(String code) {
        for (MotionEvent value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return STOP;
    }

    // 调用规则
    public static String getInvocationRules() {
        return """
                调用输出MixLLMEvent的JSON格式例如：
                {
                  "eventType": "motion",
                  "event": {
                    "type": "左转",
                    "value": "60.5"
                  }
                }
                其中转向事件需要输入旋转角度，移动事件需要输入移动的步数。
                """;
    }

    public static void main(String[] args) {
        System.out.println(AICallEnum.getAIDocs(MotionEvent.class));
    }
}
