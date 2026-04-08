package com.openapi.utils;

public class ByteUtils {

    /**
     * int转byte[]
     * @param value     待转换的int值
     * @param buffer    字节数组
     * @param offset    偏移量
     */
    public static void writeInt(int value, byte[] buffer, int offset) {
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) value;
    }

    /**
     * byte[]转int
     * @param data      字节数组
     * @param offset    偏移量
     * @return          int值
     */
    public static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
                ((data[offset + 1] & 0xFF) << 16) |
                ((data[offset + 2] & 0xFF) << 8) |
                (data[offset + 3] & 0xFF);
    }

    /**
     * short转byte[]
     * @param value     待转换的short值
     * @param buffer    字节数组
     * @param offset    偏移量
     */
    public static void writeShort(short value, byte[] buffer, int offset) {
        buffer[offset] = (byte) (value >> 8);
        buffer[offset + 1] = (byte) value;
    }

    /**
     * byte[]转short
     * @param data      字节数组
     * @param offset    偏移量
     * @return          short值
     */
    public static short readShort(byte[] data, int offset) {
        return (short) (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF));
    }

    /**
     * 计算CRC16校验和 (跳过指定范围)
     */
    public static short calculateCRC16(byte[] data, int start, int skipStart, int skipEnd, int end) {
        int crc = 0xFFFF;

        // 计算第一部分: start 到 skipStart
        for (int i = start; i < skipStart; i++) {
            crc = updateCRC16(crc, data[i]);
        }

        // 跳过 skipStart 到 skipEnd 的范围

        // 计算第二部分: skipEnd 到 end
        for (int i = skipEnd; i < end; i++) {
            crc = updateCRC16(crc, data[i]);
        }

        return (short) crc;
    }

    /**
     * 更新CRC16计算
     */
    private static int updateCRC16(int crc, byte b) {
        crc ^= (b & 0xFF);
        for (int j = 0; j < 8; j++) {
            if ((crc & 0x0001) != 0) {
                crc >>= 1;
                crc ^= 0xA001;
            } else {
                crc >>= 1;
            }
        }
        return crc;
    }

}
