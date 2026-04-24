package com.demo.aarlib.live;

/**
 * Live 推流错误码定义与文案映射。
 */
public final class LiveErrorCode {
    public static final int ERROR_VIDEO_ENCODER_OPEN = 0x01;
    public static final int ERROR_VIDEO_ENCODER_ENCODE = 0x02;
    public static final int ERROR_AUDIO_ENCODER_OPEN = 0x03;
    public static final int ERROR_AUDIO_ENCODER_ENCODE = 0x04;
    public static final int ERROR_RTMP_CONNECT_SERVER = 0x05;
    public static final int ERROR_RTMP_CONNECT_STREAM = 0x06;
    public static final int ERROR_RTMP_SEND_PACKET = 0x07;

    /**
     * 工具类不允许实例化。
     */
    private LiveErrorCode() {
    }

    /**
     * 将错误码映射为可读文案。
     *
     * @param errorCode 错误码
     * @return 错误描述
     */
    public static String messageFor(int errorCode) {
        switch (errorCode) {
            case ERROR_VIDEO_ENCODER_OPEN:
                return "x264 encoder init failed";
            case ERROR_VIDEO_ENCODER_ENCODE:
                return "x264 encode failed";
            case ERROR_AUDIO_ENCODER_OPEN:
                return "faac encoder init failed";
            case ERROR_AUDIO_ENCODER_ENCODE:
                return "faac encode failed";
            case ERROR_RTMP_CONNECT_SERVER:
                return "RTMP server connect failed";
            case ERROR_RTMP_CONNECT_STREAM:
                return "RTMP stream connect failed";
            case ERROR_RTMP_SEND_PACKET:
                return "RTMP send packet failed";
            default:
                return "Unknown live push error";
        }
    }
}
