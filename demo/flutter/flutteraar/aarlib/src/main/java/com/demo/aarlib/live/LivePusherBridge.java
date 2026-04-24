package com.demo.aarlib.live;

import android.util.Log;

/**
 * 实时推流桥接类：
 * 向上暴露纯 Java API，向下通过 JNI 调用 native 推流实现（x264/faac/RTMP）。
 */
public final class LivePusherBridge {
    private static final String TAG = "LivePusherBridge";

    static {
        System.loadLibrary("aar_live");
    }

    private final LivePushConfig config;
    private final LivePushListener listener;
    private boolean mute;
    private boolean released;
    private boolean started;

    /**
     * 创建实时推流实例并初始化音视频编码参数。
     *
     * @param config   推流参数
     * @param listener 错误回调监听
     */
    public LivePusherBridge(LivePushConfig config, LivePushListener listener) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.listener = listener;
        Log.i(TAG, "constructor: init native pusher");
        native_init();
        native_setVideoCodecInfo(
                config.getVideoWidth(),
                config.getVideoHeight(),
                config.getVideoFrameRate(),
                config.getVideoBitrate());
        native_setAudioCodecInfo(config.getAudioSampleRate(), config.getAudioChannels());
    }

    /**
     * 启动 RTMP 推流连接。
     *
     * @param liveUrl RTMP 地址
     */
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
        Log.i(TAG, "startPush, url=" + liveUrl);
    }

    /**
     * 停止推流（不释放实例）。
     */
    public synchronized void stopPush() {
        if (released || !started) {
            return;
        }
        native_stop();
        started = false;
        Log.i(TAG, "stopPush");
    }

    /**
     * 释放推流实例和 native 资源。
     */
    public synchronized void release() {
        if (released) {
            return;
        }
        stopPush();
        native_release();
        released = true;
        Log.i(TAG, "release");
    }

    /**
     * 获取音频编码器期望的输入采样点数。
     */
    public int getInputSamples() {
        ensureAvailable();
        return native_getInputSamples();
    }

    /**
     * 获取音频输入字节数（16bit PCM：samples * 2）。
     */
    public int getAudioInputByteCount() {
        int inputSamples = getInputSamples();
        return inputSamples <= 0 ? inputSamples : inputSamples * 2;
    }

    /**
     * 推送一帧视频数据到 native 编码与发送链路。
     *
     * @param data        视频帧
     * @param frameFormat 帧格式（NV21/I420）
     */
    public void pushVideoFrame(byte[] data, int frameFormat) {
        ensureStarted();
        if (data == null || data.length == 0) {
            return;
        }
        native_pushVideo(data, frameFormat);
    }

    /**
     * 推送一帧 PCM 音频数据到 native 编码与发送链路。
     */
    public void pushAudioFrame(byte[] data) {
        ensureStarted();
        if (mute || data == null || data.length == 0) {
            return;
        }
        native_pushAudio(data);
    }

    /**
     * 动态更新视频编码参数（用于方向切换后的宽高变化）。
     */
    public synchronized void updateVideoCodecInfo(int width, int height) {
        ensureAvailable();
        Log.i(TAG, "updateVideoCodecInfo, width=" + width + ", height=" + height);
        native_setVideoCodecInfo(width, height, config.getVideoFrameRate(), config.getVideoBitrate());
    }

    /**
     * 设置是否静音（仅影响 pushAudioFrame 是否上送）。
     */
    public void setMute(boolean mute) {
        this.mute = mute;
        Log.i(TAG, "setMute, mute=" + mute);
    }

    /**
     * 返回当前推流状态。
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * 校验实例是否可用。
     */
    private void ensureAvailable() {
        if (released) {
            throw new IllegalStateException("LivePusherBridge has been released");
        }
    }

    /**
     * 校验推流是否已启动。
     */
    private void ensureStarted() {
        ensureAvailable();
        if (!started) {
            throw new IllegalStateException("Push has not started");
        }
    }

    /**
     * native 错误回调入口：停止状态并通知上层监听器。
     *
     * @param errCode native 错误码
     */
    @SuppressWarnings("unused")
    private void errorFromNative(int errCode) {
        started = false;
        Log.e(TAG, "errorFromNative, errCode=" + errCode);
        if (listener != null) {
            listener.onError(errCode, LiveErrorCode.messageFor(errCode));
        }
    }

    /**
     * 初始化 native 推流对象。
     */
    private native void native_init();

    /**
     * 发起 RTMP 连接并开始发送。
     */
    private native void native_start(String path);

    /**
     * 设置视频编码参数。
     */
    private native void native_setVideoCodecInfo(int width, int height, int fps, int bitrate);

    /**
     * 设置音频编码参数。
     */
    private native void native_setAudioCodecInfo(int sampleRateInHz, int channels);

    /**
     * 获取音频编码器输入采样点数。
     */
    private native int native_getInputSamples();

    /**
     * 推送 PCM 音频数据。
     */
    private native void native_pushAudio(byte[] data);

    /**
     * 推送视频帧数据。
     */
    private native void native_pushVideo(byte[] yuv, int frameFormat);

    /**
     * 停止推流发送。
     */
    private native void native_stop();

    /**
     * 释放 native 推流对象。
     */
    private native void native_release();
}
