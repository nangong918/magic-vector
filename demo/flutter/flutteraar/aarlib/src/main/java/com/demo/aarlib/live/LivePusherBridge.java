package com.demo.aarlib.live;

public final class LivePusherBridge {
    static {
        System.loadLibrary("aar_live");
    }

    private final LivePushConfig config;
    private final LivePushListener listener;
    private boolean mute;
    private boolean released;
    private boolean started;

    public LivePusherBridge(LivePushConfig config, LivePushListener listener) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.listener = listener;
        native_init();
        native_setVideoCodecInfo(
                config.getVideoWidth(),
                config.getVideoHeight(),
                config.getVideoFrameRate(),
                config.getVideoBitrate());
        native_setAudioCodecInfo(config.getAudioSampleRate(), config.getAudioChannels());
    }

    public synchronized void startPush(String liveUrl) {
        ensureAvailable();
        if (liveUrl == null || liveUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("liveUrl must not be empty");
        }
        if (started) {
            return;
        }
        native_start(liveUrl.trim());
        started = true;
    }

    public synchronized void stopPush() {
        if (released || !started) {
            return;
        }
        native_stop();
        started = false;
    }

    public synchronized void release() {
        if (released) {
            return;
        }
        stopPush();
        native_release();
        released = true;
    }

    public int getInputSamples() {
        ensureAvailable();
        return native_getInputSamples();
    }

    public int getAudioInputByteCount() {
        int inputSamples = getInputSamples();
        return inputSamples <= 0 ? inputSamples : inputSamples * 2;
    }

    public void pushVideoFrame(byte[] data, int frameFormat) {
        ensureStarted();
        if (data == null || data.length == 0) {
            return;
        }
        native_pushVideo(data, frameFormat);
    }

    public void pushAudioFrame(byte[] data) {
        ensureStarted();
        if (mute || data == null || data.length == 0) {
            return;
        }
        native_pushAudio(data);
    }

    public synchronized void updateVideoCodecInfo(int width, int height) {
        ensureAvailable();
        native_setVideoCodecInfo(width, height, config.getVideoFrameRate(), config.getVideoBitrate());
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public boolean isStarted() {
        return started;
    }

    private void ensureAvailable() {
        if (released) {
            throw new IllegalStateException("LivePusherBridge has been released");
        }
    }

    private void ensureStarted() {
        ensureAvailable();
        if (!started) {
            throw new IllegalStateException("Push has not started");
        }
    }

    @SuppressWarnings("unused")
    private void errorFromNative(int errCode) {
        started = false;
        if (listener != null) {
            listener.onError(errCode, LiveErrorCode.messageFor(errCode));
        }
    }

    private native void native_init();

    private native void native_start(String path);

    private native void native_setVideoCodecInfo(int width, int height, int fps, int bitrate);

    private native void native_setAudioCodecInfo(int sampleRateInHz, int channels);

    private native int native_getInputSamples();

    private native void native_pushAudio(byte[] data);

    private native void native_pushVideo(byte[] yuv, int frameFormat);

    private native void native_stop();

    private native void native_release();
}
