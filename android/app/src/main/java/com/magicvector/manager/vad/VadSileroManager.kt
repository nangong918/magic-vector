package com.magicvector.manager.vad

import android.content.Context
import android.os.CountDownTimer
import com.data.domain.constant.BaseConstant
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import com.magicvector.manager.vad.VoiceRecorder.AudioCallback

class VadSileroManager {

    private lateinit var vad: VadSilero
    private lateinit var recorder: VoiceRecorder

    private val DEFAULT_SAMPLE_RATE = SampleRate.SAMPLE_RATE_8K
    private val DEFAULT_FRAME_SIZE = FrameSize.FRAME_SIZE_256
    private val DEFAULT_MODE = Mode.NORMAL
    private val DEFAULT_SILENCE_DURATION_MS = 200
    private val DEFAULT_SPEECH_DURATION_MS = 50

    fun init(context: Context, vadDetectionCallback: VadDetectionCallback) {
        this.vadDetectionCallback = vadDetectionCallback

        vad = Vad.Companion.builder()
            .setContext(context)
            .setSampleRate(DEFAULT_SAMPLE_RATE)
            .setFrameSize(DEFAULT_FRAME_SIZE)
            .setMode(DEFAULT_MODE)
            .setSilenceDurationMs(DEFAULT_SILENCE_DURATION_MS)
            .setSpeechDurationMs(DEFAULT_SPEECH_DURATION_MS)
            .build()

        recorder = VoiceRecorder(getOnAudioCallback())
    }

    private var isSpeech = false
    private var isSpeechLastTime = false
    private var vadDetectionCallback: VadDetectionCallback? = null

    private fun getOnAudioCallback(): AudioCallback{

        return object : AudioCallback {
            override fun onAudio(audioData: ShortArray) {
                isSpeechLastTime = isSpeech
                if (vad.isSpeech(audioData)) {
                    isSpeech = true

                    // 之前不是语音
                    if (!isSpeechLastTime){
                        // 直接发送吧，前端不好做逻辑，后端自己去判断：如果当前正在接收数据流，然后再接收到Start就无视
                        vadDetectionCallback?.onStartSpeech(audioData)
                    }

                    // 语音活动时，重置计时器
//                    resetSpeechTimer()
                }
                else {
                    // 延迟停止
                    isSpeech = false

                    // 之前是语音
                    if (isSpeechLastTime){
                        // 发送停止语音
                        vadDetectionCallback?.onStopSpeech()
                    }
                }
            }
        }
    }
/*

    // 为了防止VAD检测过程中多次出现静音横跳，应该设置一个倒计时100ms，如果超过限制时间之后不是Speech就判定为停止
    private var speechTimer: CountDownTimer? = null
    private fun resetSpeechTimer() {
        // 如果计时器已经在运行，先取消它
        speechTimer?.cancel()

        // 启动新的计时器
        speechTimer = object : CountDownTimer(
            BaseConstant.VAD.SILENCE_DURATION_MS,
            BaseConstant.VAD.SILENCE_DURATION_MS) {
            override fun onTick(millisUntilFinished: Long) {
                // 不做任何操作，等待时间结束
            }

            override fun onFinish() {
                // 超过200ms没有语音，执行停止逻辑
                isSpeech = false
                // 在这里调用 VAD 检测回调的停止方法
                vadDetectionCallback?.onStopSpeech()
            }
        }.start()
    }
*/

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