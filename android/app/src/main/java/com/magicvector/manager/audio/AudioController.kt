package com.magicvector.manager.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import com.data.domain.constant.BaseConstant
import com.magicvector.manager.RealtimeChatController.Companion.TAG
import com.magicvector.manager.audio.vad.VadSileroManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Audio管理者，负责维护Audio录音，播放，VAD
 */
class AudioController(
    val audioHandleCallback: AudioHandleCallback
) {

    private var realtimeChatAudioRecord: AudioRecord? = null
    private var realtimeChatAudioTrack: AudioTrack? = null
    private var vadSileroManager: VadSileroManager? = null

    /**
     * 初始化AudioRecord和AudioTrack
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initAudioRecorderAndPlayer(){
        // 配置音频参数
        val inChannelConfig = AudioFormat.CHANNEL_IN_MONO
        val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val audioRecordBufferSize = AudioRecord.getMinBufferSize(BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE, inChannelConfig, audioFormat)
        val audioTrackBufferSize = AudioTrack.getMinBufferSize(BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE, outChannelConfig, audioFormat)

        // 创建AudioRecord
        realtimeChatAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE,
            inChannelConfig,
            audioFormat,
            audioRecordBufferSize
        )

        // 创建AudioTrack
        realtimeChatAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE,
            outChannelConfig,
            audioFormat,
            audioTrackBufferSize,
            AudioTrack.MODE_STREAM
        )
    }

    /**
     * 开始播放音频
     */
    fun startAudioTrackPlay(){
        realtimeChatAudioTrack?.let { audioTrack ->
            // 清空播放缓存
            audioTrack.flush()
            audioTrack.play()
        } ?: run {
            Log.e(TAG, "AudioTrack is null")
        }
    }

    /**
     * 暂止播放音频
     */
    fun stopAudioTrackPlay(){
        realtimeChatAudioTrack?.stop() ?: run {
            Log.e(TAG, "AudioTrack is null")
        }
    }

    /**
     * 播放Base64音频数据     在播放开始之前需要先执行初始化 + flush + play；结束后需要 执行stop
     * @param base64Audio   Base64音频数据
     * @param isShowLog      是否显示日志
     */
    fun playBase64Audio(base64Audio: String, isShowLog: Boolean = false) {
        val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)

        // 写入音频数据
        realtimeChatAudioTrack?.write(
            audioBytes,
            0,
            audioBytes.size
        )

        if (isShowLog) {
            Log.i(TAG, "播放音频数据::: ${audioBytes.take(50)}")
        }

        audioHandleCallback.onPlayBase64Audio(base64Audio)
    }

    /**
     * 开始录制音频   (可以考虑改为Kotlin的Flow)
     * @param isRecording   是否正在录制
     * @param scope         协程作用域
     */
    fun startRecordingAudio(isRecording: AtomicBoolean, scope: CoroutineScope) {
        // 录制音频 -> 音频流bytes实时转为Base64的PCM格式 -> 调用websocket的sendAudioMessage
        val bufferSize = AudioRecord.getMinBufferSize(
            BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioBuffer = ByteArray(bufferSize)

        realtimeChatAudioRecord?.let { audioRecord ->
            // 开始录制
            audioRecord.startRecording()

            // 启动：发送启动录音
            audioHandleCallback.onStartRecording()

            // 使用协程录制音频
            scope.launch(Dispatchers.IO) {
                try {
                    // 检查录音状态是否正常启动
                    if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        Log.e(TAG, "AudioRecord failed to start recording")
                        audioHandleCallback.onStopRecording()
                        return@launch
                    }

                    while (isRecording.get()) {
                        val readSize = audioRecord.read(
                            audioBuffer,
                            0,
                            bufferSize
                        )

                        when {
                            readSize > 0 -> {
                                val base64Audio = Base64.encodeToString(audioBuffer, 0, readSize, Base64.NO_WRAP)
                                // 数据对外公开
                                audioHandleCallback.onObtainAudio(base64Audio)
                            }
                            readSize == AudioRecord.ERROR_INVALID_OPERATION -> {
                                Log.e(TAG, "AudioRecord ERROR_INVALID_OPERATION")
                                break
                            }
                            readSize == AudioRecord.ERROR_BAD_VALUE -> {
                                Log.e(TAG, "AudioRecord ERROR_BAD_VALUE")
                                break
                            }
                            readSize == AudioRecord.ERROR -> {
                                Log.e(TAG, "AudioRecord ERROR")
                                break
                            }
                            // readSize == 0 表示没有数据，继续循环
                        }

                        // 让出线程，避免过度占用CPU
                        // ⭐ 这里会检查协程是否被取消，如果被取消会抛出 CancellationException
                        yield()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Recording error: ${e.message}", e)
                } finally {
                    // 停止录音
                    try {
                        if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord.stop()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "AudioRecord stop error: ${e.message}")
                    }

                    // 停止：发送停止录音
                    audioHandleCallback.onStopRecording()
                }
            }
            // ⭐ 注意：这里移除了 .start() 调用！
        } ?: run {
            Log.e(TAG, "AudioRecord is null")
            return
        }
    }

    fun releaseAudioRecord(){
        realtimeChatAudioRecord?.let {
            try {
                it.stop()
                it.release()
                realtimeChatAudioRecord = null
            } catch (e: Exception){
                Log.e(TAG, "releaseAllResource::realtimeChatAudioRecord error", e)
            }
        }
    }
    fun releaseAudioTrack(){
        realtimeChatAudioTrack?.let {
            try {
                it.stop()
                it.release()
                realtimeChatAudioTrack = null
            } catch (e: Exception){
                Log.e(TAG, "releaseAllResource::realtimeChatAudioTrack error", e)
            }
        }
    }

}