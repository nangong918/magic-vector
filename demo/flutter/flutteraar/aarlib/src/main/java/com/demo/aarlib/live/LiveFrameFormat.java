package com.demo.aarlib.live;

/**
 * 视频帧格式常量定义。
 */
public final class LiveFrameFormat {
    /** NV21 格式（常见于 Camera1/部分采集链路）。 */
    public static final int NV21 = 1;
    /** I420 格式（当前 Demo Camera2 输出后送入 native 的格式）。 */
    public static final int I420 = 2;

    /**
     * 工具类不允许实例化。
     */
    private LiveFrameFormat() {
    }
}
