package com.openapi.domain.constant.tools;

/**
 * @author 13225
 * @date 2025/11/10 11:36
 */
public enum MotionEvent {
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

    MotionEvent(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getAIDocs(String className){
        StringBuilder sb = new StringBuilder();
        sb.append(className).append("{\n");
        for (MotionEvent value : values()) {
            sb.append(value).append(":");
            sb.append(value.name).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(getAIDocs(MotionEvent.class.getSimpleName()));
    }
}
