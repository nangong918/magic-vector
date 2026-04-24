package com.demo.aarlib.live.ffmpeg;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FFmpegPushBridge {
    public interface Callback {
        void onCompleted(int resultCode, String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    static {
        System.loadLibrary("aar_live");
    }

    private FFmpegPushBridge() {
    }

    public static int pushStream(String inputPath, String liveUrl) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("inputPath must not be empty");
        }
        if (liveUrl == null || liveUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("liveUrl must not be empty");
        }
        return nativePushStream(inputPath.trim(), liveUrl.trim());
    }

    public static void pushStreamAsync(String inputPath, String liveUrl, Callback callback) {
        EXECUTOR.execute(() -> {
            int resultCode = pushStream(inputPath, liveUrl);
            if (callback == null) {
                return;
            }
            MAIN_HANDLER.post(() -> callback.onCompleted(resultCode, buildMessage(resultCode)));
        });
    }

    private static String buildMessage(int resultCode) {
        return resultCode >= 0 ? "FFmpeg push finished" : "FFmpeg push failed, code=" + resultCode;
    }

    private static native int nativePushStream(String inputPath, String liveUrl);
}
