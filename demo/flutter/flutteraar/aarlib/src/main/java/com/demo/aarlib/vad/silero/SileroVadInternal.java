package com.demo.aarlib.vad.silero;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.demo.aarlib.vad.common.AudioFrameUtils;
import com.demo.aarlib.vad.common.VadEventListener;
import com.demo.aarlib.vad.common.VoiceRecorder;
import com.konovalov.vad.silero.Vad;
import com.konovalov.vad.silero.VadSilero;
import com.konovalov.vad.silero.config.FrameSize;
import com.konovalov.vad.silero.config.Mode;
import com.konovalov.vad.silero.config.SampleRate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SileroVadInternal {
    private static final SampleRate DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_8K;
    private static final FrameSize DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_256;
    private static final Mode DEFAULT_MODE = Mode.NORMAL;
    private static final int DEFAULT_SILENCE_DURATION_MS = 300;
    private static final int DEFAULT_SPEECH_DURATION_MS = 50;

    private SampleRate sampleRate = DEFAULT_SAMPLE_RATE;
    private FrameSize frameSize = DEFAULT_FRAME_SIZE;
    private Mode mode = DEFAULT_MODE;

    private VadSilero vad;
    private VoiceRecorder recorder;
    private boolean running;
    private boolean lastSpeech;
    private VadEventListener eventListener;

    void setEventListener(VadEventListener listener) {
        this.eventListener = listener;
    }

    void clearEventListener() {
        this.eventListener = null;
    }

    boolean hasRecordPermission(Context context) {
        return context != null && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    List<String> getSampleRates() {
        List<String> result = new ArrayList<>();
        for (SampleRate value : SampleRate.values()) {
            result.add(value.name());
        }
        return result;
    }

    List<String> getFrameSizes() {
        return getFrameSizes(sampleRate.name());
    }

    List<String> getFrameSizes(String sampleRateName) {
        SampleRate targetRate = parseSampleRate(sampleRateName);
        List<String> result = new ArrayList<>();
        // Keep frame options consistent with the original Silero demo setup.
        if (targetRate == SampleRate.SAMPLE_RATE_8K) {
            addIfExists(result, "FRAME_SIZE_256");
            addIfExists(result, "FRAME_SIZE_512");
            addIfExists(result, "FRAME_SIZE_768");
        } else if (targetRate == SampleRate.SAMPLE_RATE_16K) {
            addIfExists(result, "FRAME_SIZE_512");
            addIfExists(result, "FRAME_SIZE_1024");
            addIfExists(result, "FRAME_SIZE_1536");
        }
        if (!result.isEmpty()) {
            return result;
        }
        for (FrameSize value : FrameSize.values()) {
            result.add(value.name());
        }
        return result;
    }

    List<String> getModes() {
        List<String> result = new ArrayList<>();
        for (Mode value : Mode.values()) {
            result.add(value.name());
        }
        return result;
    }

    void updateConfig(String sampleRateName, String frameSizeName, String modeName) {
        sampleRate = parseSampleRate(sampleRateName);
        frameSize = parseFrameSize(sampleRate, frameSizeName);
        mode = parseMode(modeName);
    }

    void start(Context context) {
        if (context == null) {
            emit("error", "context不能为空", null);
            return;
        }
        if (!hasRecordPermission(context)) {
            emit("error", "缺少录音权限", null);
            return;
        }
        ensureVad(context.getApplicationContext());
        stop();
        lastSpeech = false;
        recorder = new VoiceRecorder(audioData -> {
            boolean speech = vad.isSpeech(audioData);
            byte[] bytes = AudioFrameUtils.shortArrayToBytes(audioData);
            if (speech && !lastSpeech) {
                emit("start_speech", "检测到开始说话", extras(bytes));
            } else if (speech) {
                emit("speeching", "持续说话中", extras(bytes));
            } else if (lastSpeech) {
                emit("stop_speech", "检测到停止说话", null);
            }
            lastSpeech = speech;
        });
        recorder.start(sampleRate.getValue(), frameSize.getValue());
        running = true;
        emit("state", "silero已开始监听", stateExtras(true));
    }

    void stop() {
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
        running = false;
        emit("state", "silero已停止监听", stateExtras(false));
    }

    void release() {
        stop();
        if (vad != null) {
            vad.close();
            vad = null;
        }
    }

    private void ensureVad(Context context) {
        if (vad != null) {
            vad.close();
            vad = null;
        }
        vad = Vad.builder()
                .setContext(context)
                .setSampleRate(sampleRate)
                .setFrameSize(frameSize)
                .setMode(mode)
                .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
                .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
                .build();
    }

    private SampleRate parseSampleRate(String value) {
        try {
            return SampleRate.valueOf(value);
        } catch (Exception e) {
            return DEFAULT_SAMPLE_RATE;
        }
    }

    private FrameSize parseFrameSize(SampleRate targetRate, String value) {
        List<String> supported = getFrameSizes(targetRate.name());
        if (supported != null && !supported.isEmpty() && (value == null || !supported.contains(value))) {
            return FrameSize.valueOf(supported.get(0));
        }
        try {
            return FrameSize.valueOf(value);
        } catch (Exception e) {
            return DEFAULT_FRAME_SIZE;
        }
    }

    private Mode parseMode(String value) {
        try {
            return Mode.valueOf(value);
        } catch (Exception e) {
            return DEFAULT_MODE;
        }
    }

    private void addIfExists(List<String> result, String frameName) {
        try {
            result.add(FrameSize.valueOf(frameName).name());
        } catch (Exception ignore) {
        }
    }

    private Map<String, Object> stateExtras(boolean isRunning) {
        Map<String, Object> extras = new HashMap<>();
        extras.put("running", isRunning);
        extras.put("sampleRate", sampleRate.name());
        extras.put("frameSize", frameSize.name());
        extras.put("mode", mode.name());
        return extras;
    }

    private Map<String, Object> extras(byte[] buffer) {
        Map<String, Object> extras = new HashMap<>();
        extras.put("audioBuffer", buffer);
        return extras;
    }

    private void emit(String type, String message, Map<String, Object> extras) {
        if (eventListener == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("message", message);
        if (extras != null) {
            payload.putAll(extras);
        }
        eventListener.onEvent(payload);
    }
}
