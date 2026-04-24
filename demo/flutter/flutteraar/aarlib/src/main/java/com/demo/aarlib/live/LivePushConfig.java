package com.demo.aarlib.live;

public final class LivePushConfig {
    private final int videoWidth;
    private final int videoHeight;
    private final int videoBitrate;
    private final int videoFrameRate;
    private final int audioSampleRate;
    private final int audioChannels;

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

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public int getAudioChannels() {
        return audioChannels;
    }
}
