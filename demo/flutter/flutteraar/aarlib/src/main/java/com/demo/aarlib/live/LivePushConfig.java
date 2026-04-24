package com.demo.aarlib.live;

/**
 * 实时推流配置对象：
 * 描述编码器与采集侧需要的基础参数。
 */
public final class LivePushConfig {
    private final int videoWidth;
    private final int videoHeight;
    private final int videoBitrate;
    private final int videoFrameRate;
    private final int audioSampleRate;
    private final int audioChannels;

    /**
     * 构造推流配置。
     */
    public LivePushConfig(
            int videoWidth,
            int videoHeight,
            int videoBitrate,
            int videoFrameRate,
            int audioSampleRate,
            int audioChannels) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoBitrate = videoBitrate;
        this.videoFrameRate = videoFrameRate;
        this.audioSampleRate = audioSampleRate;
        this.audioChannels = audioChannels;
    }

    /**
     * @return 视频宽度
     */
    public int getVideoWidth() {
        return videoWidth;
    }

    /**
     * @return 视频高度
     */
    public int getVideoHeight() {
        return videoHeight;
    }

    /**
     * @return 视频码率（bit/s）
     */
    public int getVideoBitrate() {
        return videoBitrate;
    }

    /**
     * @return 视频帧率（fps）
     */
    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    /**
     * @return 音频采样率
     */
    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    /**
     * @return 音频声道数
     */
    public int getAudioChannels() {
        return audioChannels;
    }
}
