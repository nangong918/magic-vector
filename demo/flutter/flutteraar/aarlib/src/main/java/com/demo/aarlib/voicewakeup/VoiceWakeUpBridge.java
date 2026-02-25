package com.demo.aarlib.voicewakeup;

import android.content.Context;

import java.util.Map;

public final class VoiceWakeUpBridge {
    private static final VoiceWakeUpInternal internal = new VoiceWakeUpInternal();

    private VoiceWakeUpBridge() {
    }

    public static final class Config extends VoiceWakeUpConfig {
        public Config(String appId, String apiKey, String apiSecret, String workDir, String abilityId) {
            super(appId, apiKey, apiSecret, workDir, abilityId);
        }
    }

    public static void setDefaultConfig(VoiceWakeUpConfig config) {
        internal.setConfig(config);
    }

    public static boolean hasRecordPermission(Context context) {
        return internal.hasRecordPermission(context);
    }

    public static boolean isSdkInited() {
        return internal.isSdkInited();
    }

    public static boolean isRecording() {
        return internal.isRecording();
    }

    public static void setEventListener(VoiceWakeUpEventListener listener) {
        internal.setEventListener(listener);
    }

    public static void clearEventListener() {
        internal.clearEventListener();
    }

    public static void initSdk(Context context) {
        VoiceWakeUpConfig config = internal.config;
        if (config == null) {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "error");
            payload.put("message", "未设置VoiceWakeUpBridge配置，请先调用setDefaultConfig");
            if (internal.eventListener != null) {
                internal.eventListener.onEvent(payload);
            }
            return;
        }
        initSdk(context, config);
    }

    public static void initSdk(Context context, VoiceWakeUpConfig config) {
        internal.initSdk(context, config);
    }

    public static void startRecordWake(Context context, String keyword) {
        internal.startRecordWake(context, keyword);
    }

    public static void stopRecordWake() {
        internal.stopRecordWake();
    }

    public static void startFileWake(Context context, String keyword, String filePath) {
        internal.startFileWake(context, keyword, filePath);
    }

    public static void release() {
        internal.release();
    }
}
