package com.demo.aarlib.voicewakeup;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.iflytek.aikit.core.AiAudio;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiStatus;
import com.iflytek.aikit.core.BaseLibrary;
import com.iflytek.aikit.core.CoreListener;
import com.iflytek.aikit.core.ErrType;
import com.iflytek.aikit.core.LogLvl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VoiceWakeUpBridge {
    private static final String TAG = "VoiceWakeUpBridge";
    private static final String DEFAULT_ABILITY_ID = "e867a88f2";
    private static final int BUFFER_SIZE = 1280;
    private static final int WRITE_CHUNK_SIZE = 320;
    private static final Object LOCK = new Object();

    private static HandlerThread ivwHandlerThread;
    private static Handler ivwHandler;
    private static Context appContext;

    private static String abilityId = DEFAULT_ABILITY_ID;
    private static String workDir = "";
    private static String resDir = "";
    private static String audioPath = "";

    private static AiHandle aiHandle;
    private static AudioRecord audioRecord;
    private static final AtomicBoolean isEnd = new AtomicBoolean(true);
    private static final AtomicBoolean isRecording = new AtomicBoolean(false);
    private static boolean shouldSendEndFrame = false;
    private static boolean sdkInitRequested = false;
    private static boolean abilityListenerRegistered = false;

    private static EventListener eventListener;
    private static Config defaultConfig;

    private static final CoreListener coreListener = new CoreListener() {
        @Override
        public void onAuthStateChange(ErrType type, int code) {
            if (type == ErrType.AUTH) {
                Map<String, Object> extras = new HashMap<>();
                extras.put("code", code);
                emitEvent("auth", code == 0 ? "SDK授权成功" : "SDK授权失败: " + code, extras);
            }
        }
    };

    private static final AiListener abilityListener = new AiListener() {
        @Override
        public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {
            if (outputData == null || outputData.isEmpty()) {
                return;
            }
            for (AiResponse resp : outputData) {
                String key = resp.getKey();
                String result = new String(resp.getValue(), StandardCharsets.UTF_8);
                int status = resp.getStatus();
                Map<String, Object> extras = new HashMap<>();
                extras.put("key", key);
                extras.put("status", status);
                extras.put("result", result);
                String type = ("func_wake_up".equals(key) || "func_pre_wakeup".equals(key)) ? "wakeup" : "log";
                emitEvent(type, "key=" + key + " status=" + status + " value=" + result, extras);
            }
        }

        @Override
        public void onEvent(int i, int i1, List<AiResponse> list, Object o) {
            emitEvent("log", "onEvent: " + i + ", event=" + i1, null);
        }

        @Override
        public void onError(int i, int i1, String s, Object o) {
            emitEvent("error", "能力执行错误: code=" + i1 + ", msg=" + (s == null ? "" : s), null);
        }
    };

    private VoiceWakeUpBridge() {
    }

    public interface EventListener {
        void onEvent(Map<String, Object> payload);
    }

    public static final class Config {
        private final String appId;
        private final String apiKey;
        private final String apiSecret;
        private final String workDir;
        private final String abilityId;

        public Config(String appId, String apiKey, String apiSecret, String workDir, String abilityId) {
            this.appId = appId;
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
            this.workDir = workDir;
            this.abilityId = abilityId;
        }
    }

    public static void setDefaultConfig(Config config) {
        synchronized (LOCK) {
            defaultConfig = config;
        }
    }

    public static boolean hasRecordPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isSdkInited() {
        synchronized (LOCK) {
            return sdkInitRequested;
        }
    }

    public static boolean isRecording() {
        return isRecording.get();
    }

    public static void setEventListener(EventListener listener) {
        synchronized (LOCK) {
            eventListener = listener;
        }
    }

    public static void clearEventListener() {
        synchronized (LOCK) {
            eventListener = null;
        }
    }

    public static void initSdk(Context context) {
        Config config;
        synchronized (LOCK) {
            config = defaultConfig;
        }
        if (config == null) {
            emitEvent("error", "未设置VoiceWakeUpBridge配置，请先调用setDefaultConfig", null);
            return;
        }
        initSdk(context, config);
    }

    public static void initSdk(Context context, Config config) {
        if (context == null || config == null) {
            emitEvent("error", "initSdk参数无效", null);
            return;
        }
        ensureWorkerThread();
        final Context app = context.getApplicationContext();
        ivwHandler.post(() -> initSdkInternal(app, config));
    }

    public static void startRecordWake(Context context, String keyword) {
        if (context == null) {
            emitEvent("error", "startRecordWake参数无效", null);
            return;
        }
        if (!hasRecordPermission(context)) {
            emitEvent("error", "缺少录音权限: RECORD_AUDIO", null);
            return;
        }
        ensureWorkerThread();
        final String keywordInput = TextUtils.isEmpty(keyword) ? "你好小迪" : keyword;
        ivwHandler.post(() -> {
            int ret = startSession(keywordInput);
            if (ret != 0) {
                emitEvent("error", "启动录音唤醒失败: " + ret, null);
                return;
            }
            startRecordLoop();
        });
    }

    public static void stopRecordWake() {
        ensureWorkerThread();
        ivwHandler.post(() -> {
            if (isRecording.get()) {
                shouldSendEndFrame = true;
            } else {
                endSession();
            }
        });
    }

    public static void startFileWake(Context context, String keyword, String filePath) {
        if (context == null || TextUtils.isEmpty(filePath)) {
            emitEvent("error", "startFileWake参数无效", null);
            return;
        }
        ensureWorkerThread();
        final String keywordInput = TextUtils.isEmpty(keyword) ? "你好小迪" : keyword;
        ivwHandler.post(() -> {
            int ret = startSession(keywordInput);
            if (ret != 0) {
                emitEvent("error", "启动文件唤醒失败: " + ret, null);
                return;
            }
            audioPath = filePath;
            writeByFile();
        });
    }

    public static void release() {
        ensureWorkerThread();
        ivwHandler.post(() -> {
            stopRecordInternal();
            endSession();
            unInitSdk();
            releaseAudioRecord();
        });
    }

    private static void ensureWorkerThread() {
        synchronized (LOCK) {
            if (ivwHandlerThread != null && ivwHandlerThread.isAlive() && ivwHandler != null) {
                return;
            }
            ivwHandlerThread = new HandlerThread("ivw-worker");
            ivwHandlerThread.start();
            ivwHandler = new Handler(ivwHandlerThread.getLooper());
        }
    }

    private static void initSdkInternal(Context app, Config config) {
        if (sdkInitRequested) {
            emitEvent("log", "SDK已经初始化过，无需重复初始化", null);
            return;
        }
        if (TextUtils.isEmpty(config.appId) || TextUtils.isEmpty(config.apiKey) || TextUtils.isEmpty(config.apiSecret)) {
            emitEvent("error", "SDK配置缺失: appId/apiKey/apiSecret不能为空", null);
            return;
        }
        appContext = app;
        abilityId = TextUtils.isEmpty(config.abilityId) ? DEFAULT_ABILITY_ID : config.abilityId;
        workDir = resolveWorkDir(config.workDir);
        resDir = workDir + "ivw";
        ensureWorkDirReady();
        syncIvwAssetsToWorkDir();

        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, workDir + "aikit/aeeLog.txt");
        BaseLibrary.Params params = BaseLibrary.Params.builder()
                .appId(config.appId)
                .apiKey(config.apiKey)
                .apiSecret(config.apiSecret)
                .workDir(workDir)
                .build();
        new Thread(() -> AiHelper.getInst().initEntry(appContext, params)).start();
        AiHelper.getInst().registerListener(coreListener);
        if (!abilityListenerRegistered) {
            AiHelper.getInst().registerListener(abilityId, abilityListener);
            abilityListenerRegistered = true;
        }
        sdkInitRequested = true;
        emitEvent("log", "SDK初始化已发起, workDir=" + workDir, null);
    }

    private static int startSession(String keywordInput) {
        if (!sdkInitRequested) {
            emitEvent("error", "请先调用initSdk初始化", null);
            return -1;
        }
        if (!keywordToFile(keywordInput)) {
            emitEvent("error", "唤醒词写入失败，请检查workDir权限", null);
            return -2;
        }
        AiRequest.Builder customBuilder = AiRequest.builder();
        customBuilder.customText("key_word", resDir + "/keyword.txt", 0);
        int ret = AiHelper.getInst().loadData(abilityId, customBuilder.build());
        if (ret != 0) {
            emitEvent("error", "loadData失败: " + ret, null);
            return ret;
        }
        int[] indexes = {0};
        ret = AiHelper.getInst().specifyDataSet(abilityId, "key_word", indexes);
        if (ret != 0) {
            emitEvent("error", "specifyDataSet失败: " + ret, null);
            return ret;
        }
        AiRequest.Builder paramBuilder = AiRequest.builder();
        paramBuilder.param("wdec_param_nCmThreshold", "0 0:800");
        paramBuilder.param("gramLoad", true);
        isEnd.set(false);
        aiHandle = AiHelper.getInst().start(abilityId, paramBuilder.build(), null);
        int code = aiHandle == null ? -1 : aiHandle.getCode();
        if (code != 0) {
            emitEvent("error", "start失败: " + code, null);
            return code;
        }
        Map<String, Object> extras = new HashMap<>();
        extras.put("state", "started");
        emitEvent("state", "唤醒会话已开始", extras);
        return 0;
    }

    private static void startRecordLoop() {
        createAudioRecordIfNeed();
        if (audioRecord == null) {
            emitEvent("error", "录音器创建失败", null);
            return;
        }
        shouldSendEndFrame = false;
        isRecording.set(true);
        audioRecord.startRecording();
        emitEvent("log", "开始录音送引擎", null);

        ivwHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isRecording.get()) {
                    return;
                }
                byte[] data = new byte[BUFFER_SIZE];
                int read = audioRecord.read(data, 0, BUFFER_SIZE);
                if (read > 0) {
                    byte[] sendData = read == BUFFER_SIZE ? data : Arrays.copyOf(data, read);
                    int volume = calculateVolume(sendData);
                    Map<String, Object> extras = new HashMap<>();
                    extras.put("db", Math.abs(volume));
                    emitEvent("db", "当前分贝: " + Math.abs(volume), extras);

                    AiStatus status = shouldSendEndFrame ? AiStatus.END : AiStatus.CONTINUE;
                    write(sendData, status);
                    if (status == AiStatus.END) {
                        stopRecordInternal();
                        endSession();
                        return;
                    }
                }
                ivwHandler.post(this);
            }
        });
    }

    private static void writeByFile() {
        try {
            File file = new File(audioPath);
            if (!file.exists()) {
                emitEvent("error", "音频文件不存在: " + audioPath, null);
                endSession();
                return;
            }
            byte[] audioData = readFileBytes(file);
            int leftBytes = audioData.length;
            int index = 0;
            while (leftBytes > 0) {
                int writeLen = leftBytes > WRITE_CHUNK_SIZE ? WRITE_CHUNK_SIZE : leftBytes;
                leftBytes -= writeLen;
                byte[] part = Arrays.copyOfRange(audioData, index * WRITE_CHUNK_SIZE,
                        index * WRITE_CHUNK_SIZE + writeLen);
                AiStatus status;
                if (index == 0) {
                    status = AiStatus.BEGIN;
                } else if (leftBytes == 0) {
                    status = AiStatus.END;
                } else {
                    status = AiStatus.CONTINUE;
                }
                write(part, status);
                index++;
            }
            emitEvent("log", "上传音频写入完成", null);
        } catch (Exception e) {
            emitEvent("error", "读取音频失败: " + e.getMessage(), null);
        } finally {
            endSession();
        }
    }

    private static byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int offset = 0;
            int count;
            while ((count = inputStream.read(buffer, offset, buffer.length - offset)) > 0) {
                offset += count;
                if (offset >= buffer.length) {
                    break;
                }
            }
            return buffer;
        }
    }

    private static void write(byte[] part, AiStatus status) {
        if (isEnd.get()) {
            return;
        }
        if (aiHandle == null) {
            return;
        }
        AiRequest.Builder builder = AiRequest.builder();
        AiAudio aiAudio = AiAudio.get("wav").data(part).status(status).valid();
        builder.payload(aiAudio);
        int ret = AiHelper.getInst().write(builder.build(), aiHandle);
        if (ret != 0) {
            emitEvent("error", "write失败: " + ret, null);
        }
    }

    private static void endSession() {
        if (isEnd.get()) {
            Map<String, Object> extras = new HashMap<>();
            extras.put("state", "stopped");
            emitEvent("state", "会话已结束", extras);
            return;
        }
        if (aiHandle == null) {
            return;
        }
        int ret = AiHelper.getInst().end(aiHandle);
        if (ret == 0) {
            isEnd.set(true);
            aiHandle = null;
            Map<String, Object> extras = new HashMap<>();
            extras.put("state", "stopped");
            emitEvent("state", "唤醒结束 end=" + ret, extras);
        } else {
            isEnd.set(false);
            emitEvent("error", "end失败: " + ret, null);
        }
    }

    private static boolean keywordToFile(String keywordInput) {
        try {
            File dir = new File(resDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            File keywordFile = new File(resDir + "/keyword.txt");
            if (keywordFile.exists() && !keywordFile.delete()) {
                Log.w(TAG, "failed to delete keyword.txt");
            }
            File binFile = new File(resDir + "/keyword.bin");
            if (binFile.exists() && !binFile.delete()) {
                Log.w(TAG, "failed to delete keyword.bin");
            }
            String temp = TextUtils.isEmpty(keywordInput) ? "你好小迪" : keywordInput;
            String normalized = temp.replace("，", ",");
            String[] keywords = normalized.split(",");
            if (!keywordFile.exists() && !keywordFile.createNewFile()) {
                return false;
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(keywordFile),
                    StandardCharsets.UTF_8);
                 BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                for (String item : keywords) {
                    String kw = item.trim();
                    if (!kw.isEmpty()) {
                        bufferedWriter.write(kw);
                        bufferedWriter.write(";");
                        bufferedWriter.newLine();
                    }
                }
            }
            return true;
        } catch (IOException e) {
            emitEvent("error", "关键词写文件失败: " + e.getMessage(), null);
            return false;
        }
    }

    private static String resolveWorkDir(String cfgDir) {
        if (!TextUtils.isEmpty(cfgDir)) {
            return cfgDir.endsWith(File.separator) ? cfgDir : cfgDir + File.separator;
        }
        File baseDir = appContext != null ? appContext.getExternalFilesDir(null) : null;
        if (baseDir == null && appContext != null) {
            baseDir = appContext.getFilesDir();
        }
        if (baseDir == null) {
            return File.separator;
        }
        String path = new File(baseDir, "iflytek").getAbsolutePath();
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    private static void ensureWorkDirReady() {
        File root = new File(workDir);
        File ivwDir = new File(resDir);
        File aikitDir = new File(workDir + "aikit");
        if (!root.exists()) {
            root.mkdirs();
        }
        if (!ivwDir.exists()) {
            ivwDir.mkdirs();
        }
        if (!aikitDir.exists()) {
            aikitDir.mkdirs();
        }
    }

    private static void syncIvwAssetsToWorkDir() {
        try {
            if (appContext == null) {
                emitEvent("error", "应用上下文为空，无法拷贝资源", null);
                return;
            }
            File ivwDir = new File(resDir);
            copyAssetFolder("ivw", ivwDir);
            emitEvent("log", "离线资源已同步到: " + resDir, null);
        } catch (Exception e) {
            emitEvent("error", "离线资源拷贝失败: " + e.getMessage(), null);
        }
    }

    private static void copyAssetFolder(String assetPath, File targetDir) throws IOException {
        if (appContext == null) {
            return;
        }
        String[] list = appContext.getAssets().list(assetPath);
        if (list == null) {
            return;
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        for (String name : list) {
            String childAssetPath = assetPath + "/" + name;
            String[] childList = appContext.getAssets().list(childAssetPath);
            if (childList == null || childList.length == 0) {
                copyAssetFile(childAssetPath, new File(targetDir, name));
            } else {
                copyAssetFolder(childAssetPath, new File(targetDir, name));
            }
        }
    }

    private static void copyAssetFile(String assetPath, File outFile) throws IOException {
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (appContext == null) {
            return;
        }
        try (java.io.InputStream input = appContext.getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
            }
            output.flush();
        }
    }

    private static void createAudioRecordIfNeed() {
        if (audioRecord != null) {
            return;
        }
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
        );
    }

    private static void stopRecordInternal() {
        isRecording.set(false);
        shouldSendEndFrame = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Exception e) {
                Log.w(TAG, "audioRecord stop failed: " + e.getMessage());
            }
        }
        emitEvent("log", "录音已停止", null);
    }

    private static void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception e) {
                Log.w(TAG, "audioRecord release failed: " + e.getMessage());
            } finally {
                audioRecord = null;
            }
        }
    }

    private static int calculateVolume(byte[] buffer) {
        double sumVolume = 0.0;
        for (int i = 0; i < buffer.length; i += 2) {
            if (i + 1 >= buffer.length) {
                break;
            }
            int v1 = buffer[i] & 0xFF;
            int v2 = buffer[i + 1] & 0xFF;
            int temp = v1 + (v2 << 8);
            if (temp >= 0x8000) {
                temp = 0xFFFF - temp;
            }
            sumVolume += Math.abs(temp);
        }
        double avgVolume = sumVolume / buffer.length / 2;
        return (int) (Math.log10(1 + avgVolume) * 10);
    }

    private static void unInitSdk() {
        try {
            AiHelper.getInst().unInit();
            sdkInitRequested = false;
            emitEvent("log", "SDK已逆初始化", null);
        } catch (Exception e) {
            emitEvent("error", "SDK逆初始化失败: " + e.getMessage(), null);
        }
    }

    private static void emitEvent(String type, String message, Map<String, Object> extras) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("message", message);
        if (extras != null) {
            payload.putAll(extras);
        }
        EventListener listener;
        synchronized (LOCK) {
            listener = eventListener;
        }
        if (listener != null) {
            listener.onEvent(payload);
        }
    }
}
