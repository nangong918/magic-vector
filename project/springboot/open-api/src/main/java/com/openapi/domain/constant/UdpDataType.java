package com.openapi.domain.constant;

import lombok.NonNull;

public enum UdpDataType {
    VIDEO((byte) 0x01, "视频数据"),
    AUDIO((byte) 0x02, "音频数据"),
    CONTROL((byte) 0x03, "控制指令"),
    HEARTBEAT((byte) 0x04, "心跳包");

    private final byte type;
    private final String description;

    UdpDataType(byte type, String description) {
        this.type = type;
        this.description = description;
    }

    public byte getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据字节值获取枚举
     */
    @NonNull
    public static UdpDataType fromByte(byte type) {
        for (UdpDataType dataType : values()) {
            if (dataType.type == type) {
                return dataType;
            }
        }
        throw new IllegalArgumentException("未知的数据类型: " + type);
    }
}
