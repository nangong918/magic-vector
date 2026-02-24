package com.demo.aarlib.vad.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class AudioFrameUtils {
    private AudioFrameUtils() {
    }

    public static byte[] shortArrayToBytes(Short[] src) {
        if (src == null || src.length == 0) {
            return new byte[0];
        }
        ByteBuffer buffer = ByteBuffer.allocate(src.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (Short value : src) {
            buffer.putShort(value == null ? 0 : value);
        }
        return buffer.array();
    }

    public static byte[] shortArrayToBytes(short[] src) {
        if (src == null || src.length == 0) {
            return new byte[0];
        }
        ByteBuffer buffer = ByteBuffer.allocate(src.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short value : src) {
            buffer.putShort(value);
        }
        return buffer.array();
    }
}
