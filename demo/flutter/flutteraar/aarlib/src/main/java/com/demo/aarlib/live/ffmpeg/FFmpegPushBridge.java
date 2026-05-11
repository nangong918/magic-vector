package com.demo.aarlib.live.ffmpeg;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FFmpeg 文件推流桥接类：
 * 支持同步和异步两种方式将输入媒体转推到 RTMP / RTSP。
 */
public final class FFmpegPushBridge {
    private static final String TAG = "FFmpegPushBridge";

    /**
     * 推流完成回调接口。
     */
    public interface Callback {
        /**
         * 推流任务完成后回调（无论成功或失败）。
         *
         * @param resultCode native 返回码（>=0 通常表示成功）
         * @param message    面向 UI 的结果描述
         */
        void onCompleted(int resultCode, String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    static {
        System.loadLibrary("aar_live");
    }

    /**
     * 工具类不允许实例化。
     */
    private FFmpegPushBridge() {
    }

    /**
     * 同步执行 FFmpeg 推流。
     *
     * @param inputPath 输入媒体路径（本地或网络）
     * @param outputUrl 推流目标地址（RTMP / RTSP）
     * @return native 推流返回码
     */
    public static int pushStream(String inputPath, String outputUrl) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("inputPath must not be empty");
        }
        if (outputUrl == null || outputUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("outputUrl must not be empty");
        }
        Log.i(TAG, "pushStream, input=" + inputPath + ", outputUrl=" + outputUrl);
        return nativePushStream(inputPath.trim(), outputUrl.trim());
    }

    /**
     * 异步执行 FFmpeg 推流，并在主线程触发回调。
     *
     * @param inputPath 输入媒体路径
     * @param outputUrl 推流目标地址（RTMP / RTSP）
     * @param callback  可选回调
     */
    public static void pushStreamAsync(String inputPath, String outputUrl, Callback callback) {
        EXECUTOR.execute(() -> {
            int resultCode = pushStream(inputPath, outputUrl);
            Log.i(TAG, "pushStreamAsync finished, resultCode=" + resultCode);
            if (callback == null) {
                return;
            }
            // 此处使用main线程主要是因为UI更新需要在主线程
            MAIN_HANDLER.post(() -> callback.onCompleted(resultCode, buildMessage(resultCode)));
        });
    }

    /**
     * 构建简单结果文案，供 Demo 页面显示。
     */
    private static String buildMessage(int resultCode) {
        return resultCode >= 0 ? "FFmpeg push finished" : "FFmpeg push failed, code=" + resultCode;
    }

    /**
     * JNI：调用 native FFmpeg 推流实现。
     */
    private static native int nativePushStream(String inputPath, String liveUrl);
}
