package com.demo.aarlib.vad.common;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

public class VoiceRecorder {
    private static final String TAG = "VadVoiceRecorder";

    public interface AudioCallback {
        void onAudio(short[] audioData);
    }

    private final AudioCallback callback;
    private AudioRecord audioRecord;
    private Thread thread;
    private volatile boolean listening;

    private int sampleRate;
    private int frameSize;

    public VoiceRecorder(AudioCallback callback) {
        this.callback = callback;
    }

    public synchronized void start(int sampleRate, int frameSize) {
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        stop();
        audioRecord = createAudioRecord();
        if (audioRecord == null) {
            return;
        }
        listening = true;
        audioRecord.startRecording();
        thread = new Thread(new ProcessVoice(), "vad-recorder");
        thread.start();
    }

    public synchronized void stop() {
        listening = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Exception ignore) {
            }
            audioRecord.release();
            audioRecord = null;
        }
    }

    @SuppressLint("MissingPermission")
    private AudioRecord createAudioRecord() {
        try {
            int minBufferSize = Math.max(
                    AudioRecord.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    ),
                    2 * frameSize
            );
            AudioRecord record = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize
            );
            if (record.getState() == AudioRecord.STATE_INITIALIZED) {
                return record;
            }
            record.release();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "cannot create AudioRecord", e);
        }
        return null;
    }

    private final class ProcessVoice implements Runnable {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            while (!Thread.currentThread().isInterrupted() && listening) {
                short[] buffer = new short[frameSize];
                AudioRecord record = audioRecord;
                if (record == null) {
                    return;
                }
                int read = record.read(buffer, 0, buffer.length);
                if (read > 0 && callback != null) {
                    callback.onAudio(buffer);
                }
            }
        }
    }
}
