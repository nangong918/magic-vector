package com.example.flutteraar.live.util;

/**
 * YUV 工具类：
 * 提供推流预处理常用的 I420 旋转能力。
 */
public final class YuvUtil {
    /**
     * 工具类不允许实例化。
     */
    private YuvUtil() {
    }

    /**
     * I420 顺时针旋转 90 度。
     *
     * @param dst    输出缓冲
     * @param src    输入缓冲
     * @param width  输入宽
     * @param height 输入高
     */
    public static void YUV420pRotate90(byte[] dst, byte[] src, int width, int height) {
        int n = 0;
        int wh = width * height;
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                dst[n++] = src[width * i + j];
            }
        }
        for (int i = 0; i < halfWidth; i++) {
            for (int j = 1; j <= halfHeight; j++) {
                dst[n++] = src[wh + ((halfHeight - j) * halfWidth + i)];
            }
        }
        for (int i = 0; i < halfWidth; i++) {
            for (int j = 1; j <= halfHeight; j++) {
                dst[n++] = src[wh + wh / 4 + ((halfHeight - j) * halfWidth + i)];
            }
        }
    }

    /**
     * I420 旋转 180 度。
     *
     * @param dst    输出缓冲
     * @param src    输入缓冲
     * @param width  输入宽
     * @param height 输入高
     */
    public static void YUV420pRotate180(byte[] dst, byte[] src, int width, int height) {
        int n = 0;
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        for (int j = height - 1; j >= 0; j--) {
            for (int i = width; i > 0; i--) {
                dst[n++] = src[width * j + i - 1];
            }
        }
        int offset = width * height;
        for (int j = halfHeight - 1; j >= 0; j--) {
            for (int i = halfWidth; i > 0; i--) {
                dst[n++] = src[offset + halfWidth * j + i - 1];
            }
        }
        offset += width * height / 4;
        for (int j = halfHeight - 1; j >= 0; j--) {
            for (int i = halfWidth; i > 0; i--) {
                dst[n++] = src[offset + halfWidth * j + i - 1];
            }
        }
    }
}
