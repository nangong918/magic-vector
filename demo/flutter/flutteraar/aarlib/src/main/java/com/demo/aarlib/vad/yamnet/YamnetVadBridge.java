package com.demo.aarlib.vad.yamnet;

import android.content.Context;

import com.demo.aarlib.vad.common.VadEventListener;

import java.util.List;

public final class YamnetVadBridge {
    private static final YamnetVadInternal INTERNAL = new YamnetVadInternal();

    private YamnetVadBridge() {
    }

    public static void setEventListener(VadEventListener listener) {
        INTERNAL.setEventListener(listener);
    }

    public static void clearEventListener() {
        INTERNAL.clearEventListener();
    }

    public static boolean hasRecordPermission(Context context) {
        return INTERNAL.hasRecordPermission(context);
    }

    public static List<String> getSampleRates() {
        return INTERNAL.getSampleRates();
    }

    public static List<String> getFrameSizes() {
        return INTERNAL.getFrameSizes();
    }

    public static List<String> getModes() {
        return INTERNAL.getModes();
    }

    public static void updateConfig(String sampleRate, String frameSize, String mode) {
        INTERNAL.updateConfig(sampleRate, frameSize, mode);
    }

    public static void start(Context context) {
        INTERNAL.start(context);
    }

    public static void stop() {
        INTERNAL.stop();
    }

    public static void release() {
        INTERNAL.release();
    }
}
