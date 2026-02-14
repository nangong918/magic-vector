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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class VoiceWakeUpInternal {
    private static final String TAG = "VoiceWakeUpBridge";
    private static final String DEFAULT_ABILITY_ID = "e867a88f2";
    private static final int BUFFER_SIZE = 1280;
    private static final int WRITE_CHUNK_SIZE = 320;

    private HandlerThread ivwHandlerThread;
    private Handler ivwHandler;
    private Context appContext;

    private String abilityId = DEFAULT_ABILITY_ID;
    private String workDir = "";
    private String resDir = "";
    private String audioPath = "";

    private AiHandle aiHandle;
    private AudioRecord audioRecord;
    private final AtomicBoolean isEnd = new AtomicBoolean(true);
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private boolean shouldSendEndFrame = false;
    private boolean sdkInitRequested = false;
    private boolean abilityListenerRegistered = false;

    VoiceWakeUpEventListener eventListener;
    VoiceWakeUpConfig config;

    private final CoreListener coreListener = new CoreListener() {
        @Override
        public void onAuthStateChange(ErrType type, int code) {
            if (type == ErrType.AUTH) {
                Map<String, Object> extras = new HashMap<>();
                extras.put("code", code);
                emitEvent("auth", code == 0 ? "SDK授权成功" : "SDK授权失败: " + code, extras);
            }
        }
    };

    private final AiListener abilityListener = new AiListener() {
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

    VoiceWakeUpInternal() {
    }

    void setConfig(VoiceWakeUpConfig config) {
        this.config = config;
    }

    void setEventListener(VoiceWakeUpEventListener listener) {
        this.eventListener = listener;
    }

    void clearEventListener() {
        this.eventListener = null;
    }

    boolean hasRecordPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    boolean isSdkInited() {
        return sdkInitRequested;
    }

    boolean isRecording() {
        return isRecording.get();
    }

    void initSdk(Context context, VoiceWakeUpConfig config) {
        if (context == null || config == null) {
            emitEvent("error", "initSdk参数无效", null);
            return;
        }
        ensureWorkerThread();
        final Context app = context.getApplicationContext();
        ivwHandler.post(() -> initSdkInternal(app, config));
    }

    void startRecordWake(Context context, String keyword) {
        if (context == null) {
            emitEvent("error", "startRecordWake参数无效", null);
            return;
        }
        validateKeywordOrThrow(keyword);
        if (!hasRecordPermission(context)) {
            emitEvent("error", "缺少录音权限: RECORD_AUDIO", null);
            return;
        }
        ensureWorkerThread();
        final String keywordInput = keyword;
        ivwHandler.post(() -> {
            int ret = startSession(keywordInput);
            if (ret != 0) {
                emitEvent("error", "启动录音唤醒失败: " + ret, null);
                return;
            }
            startRecordLoop();
        });
    }

    void stopRecordWake() {
        ensureWorkerThread();
        ivwHandler.post(() -> {
            if (isRecording.get()) {
                shouldSendEndFrame = true;
            } else {
                endSession();
            }
        });
    }

    void startFileWake(Context context, String keyword, String filePath) {
        if (context == null || TextUtils.isEmpty(filePath)) {
            emitEvent("error", "startFileWake参数无效", null);
            return;
        }
        validateKeywordOrThrow(keyword);
        ensureWorkerThread();
        final String keywordInput = keyword;
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

    void release() {
        ensureWorkerThread();
        ivwHandler.post(() -> {
            stopRecordInternal();
            endSession();
            unInitSdk();
            releaseAudioRecord();
        });
    }

    private void ensureWorkerThread() {
        if (ivwHandlerThread != null && ivwHandlerThread.isAlive() && ivwHandler != null) {
            return;
        }
        ivwHandlerThread = new HandlerThread("ivw-worker");
        ivwHandlerThread.start();
        ivwHandler = new Handler(ivwHandlerThread.getLooper());
    }

    private void initSdkInternal(Context app, VoiceWakeUpConfig config) {
        if (sdkInitRequested) {
            emitEvent("log", "SDK已经初始化过，无需重复初始化", null);
            return;
        }
        if (TextUtils.isEmpty(config.getAppId()) || TextUtils.isEmpty(config.getApiKey()) || TextUtils.isEmpty(config.getApiSecret())) {
            emitEvent("error", "SDK配置缺失: appId/apiKey/apiSecret不能为空", null);
            return;
        }
        appContext = app;
        abilityId = TextUtils.isEmpty(config.getAbilityId()) ? DEFAULT_ABILITY_ID : config.getAbilityId();
        IvwResourceManager resourceManager = new IvwResourceManager(appContext, config.getWorkDir());
        workDir = resourceManager.getWorkDir();
        resDir = resourceManager.getResDir();
        try {
            resourceManager.prepare();
            emitEvent("log", "离线资源已同步到: " + resDir, null);
        } catch (IOException e) {
            emitEvent("error", "离线资源准备失败: " + e.getMessage(), null);
            return;
        }

        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, workDir + "aikit/aeeLog.txt");
        BaseLibrary.Params params = BaseLibrary.Params.builder()
                .appId(config.getAppId())
                .apiKey(config.getApiKey())
                .apiSecret(config.getApiSecret())
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

    private int startSession(String keywordInput) {
        if (!sdkInitRequested) {
            emitEvent("error", "请先调用initSdk初始化", null);
            return -1;
        }
        try {
            WakeKeywordFileWriter.writeKeywordFile(resDir, keywordInput);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            emitEvent("error", "唤醒词写入失败: " + e.getMessage(), null);
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

    private void startRecordLoop() {
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

    private void writeByFile() {
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

    private byte[] readFileBytes(File file) throws IOException {
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

    private void write(byte[] part, AiStatus status) {
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

    private void endSession() {
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

    private void validateKeywordOrThrow(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            throw new IllegalArgumentException("唤醒词不能为空");
        }
    }

    private void createAudioRecordIfNeed() {
        if (audioRecord != null) {
            return;
        }
        int sampleRate = 16000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        int bufferSize = Math.max(BUFFER_SIZE * 2, minBufferSize);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);
    }

    private void releaseAudioRecord() {
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void stopRecordInternal() {
        if (isRecording.get()) {
            isRecording.set(false);
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                } catch (Exception e) {
                    Log.e(TAG, "stop audio record error", e);
                }
            }
            emitEvent("log", "录音已停止", null);
        }
    }

    private void unInitSdk() {
        if (sdkInitRequested) {
            AiHelper.getInst().unInit();
            sdkInitRequested = false;
            emitEvent("log", "SDK已释放", null);
        }
    }

    private int calculateVolume(byte[] data) {
        long sum = 0;
        for (int i = 0; i < data.length; i += 2) {
            int value = (data[i + 1] << 8) | (data[i] & 0xFF);
            sum += Math.abs(value);
        }
        return (int) (sum / (data.length / 2));
    }

    private void emitEvent(String type, String message, Map<String, Object> extras) {
        if (eventListener != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("message", message);
            if (extras != null) {
                payload.putAll(extras);
            }
            eventListener.onEvent(payload);
        }
    }
}
