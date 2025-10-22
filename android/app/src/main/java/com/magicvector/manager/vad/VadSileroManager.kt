package com.magicvector.manager.vad

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import com.magicvector.manager.vad.VoiceRecorder
import com.magicvector.manager.vad.VoiceRecorder.AudioCallback
import com.view.appview.R

class VadSileroManager {

    private lateinit var vad: VadSilero
    private lateinit var recorder: VoiceRecorder

    private val DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_8K
    private val DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_256
    private val DEFAULT_MODE = Mode.NORMAL
    private val DEFAULT_SILENCE_DURATION_MS = 300
    private val DEFAULT_SPEECH_DURATION_MS = 50

    fun init(context: Context, vadDetectionCallback: VadDetectionCallback) {
        vad = Vad.Companion.builder()
            .setContext(context)
            .setSampleRate(DEFAULT_SAMPLE_RATE)
            .setFrameSize(DEFAULT_FRAME_SIZE)
            .setMode(DEFAULT_MODE)
            .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
            .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
            .build()

        recorder = VoiceRecorder(getOnAudioCallback(
            vadDetectionCallback
        ))
    }

    private fun getOnAudioCallback(
        vadDetectionCallback: VadDetectionCallback): AudioCallback{

        return object : AudioCallback {
            override fun onAudio(audioData: ShortArray) {
                if (vad.isSpeech(audioData)) {
                    vadDetectionCallback.isSpeech(audioData)
                }
                else {
                    vadDetectionCallback.isSilence()
                }
            }
        }
    }

    // 为了防止VAD检测过程中多次出现静音横跳，应该设置一个倒计时200ms，如果超过限制时间之后不是Speech就判定为停止

    private var isRecording = false

    private fun startRecording() {
        isRecording = true
        recorder.start(vad.sampleRate.value, vad.frameSize.value)
    }

    private fun stopRecording() {
        isRecording = false
        recorder.stop()
    }

    fun onDestroy() {
        recorder.stop()
        vad.close()
    }
}