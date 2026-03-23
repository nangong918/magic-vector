package com.magicvector.manager.realtime

import android.Manifest
import android.util.Base64
import androidx.annotation.RequiresPermission
import com.data.domain.constant.VadChatState
import com.magicvector.manager.audio.AudioController
import com.magicvector.manager.audio.AudioHandleCallback
import com.magicvector.manager.audio.IsAudioRecording
import com.magicvector.manager.audio.vad.VadDetectionCallback

/**
 * 音频管理器
 * 负责 VAD 语音检测、录音、播放
 */
class RealtimeChatAudioManager(
    private val eventFlow: RealtimeChatEventFlow,
    private val wsManager: RealtimeChatWebSocketManager
) : IsAudioRecording {

    companion object {
        const val TAG = "RealtimeChatAudioManager"
    }

    var audioController: AudioController? = null

    /**
     * 初始化音频控制器
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initAudioController() {
        if (audioController == null) {
            audioController = AudioController(
                audioHandleCallback = createAudioHandleCallback(),
                vadDetectionCallback = createVadDetectionCallback()
            )
        }
        audioController?.initAudioRecorderAndPlayer()
    }

    /**
     * 开始 VAD 检测
     */
    fun startVadCall() {
        audioController?.startVAD(onStart = {
            eventFlow.emitVadState(VadChatState.Silent)
        })
    }

    /**
     * 停止 VAD 检测
     */
    fun stopVadCall() {
        audioController?.stopVAD(onStop = {
            eventFlow.emitVadState(VadChatState.Muted)
        })
    }

    /**
     * 销毁 VAD
     */
    fun destroyVadCall() {
        audioController?.releaseVADController()
        eventFlow.emitVadState(VadChatState.Muted)
    }

    /**
     * 开始播放音频轨道
     */
    fun startAudioTrackPlay() {
        audioController?.startAudioTrackPlay()
    }

    /**
     * 停止播放音频轨道
     */
    fun stopAudioTrackPlay() {
        audioController?.stopAudioTrackPlay()
    }

    /**
     * 播放 Base64 音频
     */
    fun playBase64Audio(base64Audio: String, isShowLog: Boolean = false) {
        audioController?.playBase64Audio(base64Audio, isShowLog)
    }

    /**
     * 释放所有音频资源
     */
    fun release() {
        audioController?.releaseAll()
        audioController = null
    }

    override fun isAudioRecording(): Boolean {
        return eventFlow.realtimeState.value == RealtimeChatState.RecordingAndSending
    }

    // ========== 私有方法 ==========

    private fun createAudioHandleCallback(): AudioHandleCallback {
        return object : AudioHandleCallback {
            override fun onPlayBase64Audio(base64Audio: String) {
                eventFlow.emitVadState(VadChatState.Replying)
            }

            override fun onStartRecording() {
                val dataMap = mapOf(
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.TYPE to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.type,
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.DATA to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.name
                )
                wsManager.sendSystemMessage(dataMap)
                eventFlow.updateRealtimeState(RealtimeChatState.RecordingAndSending)
            }

            override fun onObtainAudio(base64Audio: String) {
                val dataMap = mapOf(
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.TYPE to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.DATA to base64Audio
                )
                wsManager.sendSystemMessage(dataMap)
            }

            override fun onStopRecording() {
                val dataMap = mapOf(
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.TYPE to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.DATA to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                wsManager.sendSystemMessage(dataMap)
            }
        }
    }

    private fun createVadDetectionCallback(): VadDetectionCallback {
        return object : VadDetectionCallback {
            override fun onStartSpeech(audioBuffer: ByteArray) {
                sendAudioData(audioBuffer, isStart = true)
                eventFlow.emitVadState(VadChatState.Speaking)
            }

            override fun speeching(audioBuffer: ByteArray) {
                sendAudioData(audioBuffer)
                eventFlow.emitVadState(VadChatState.Speaking)
            }

            override fun onStopSpeech() {
                val dataMap = mapOf(
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.TYPE to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.DATA to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                wsManager.sendSystemMessage(dataMap)
                eventFlow.emitVadState(VadChatState.Silent)
            }
        }
    }

    private fun sendAudioData(audioBuffer: ByteArray, isStart: Boolean = false) {
        if (isStart) {
            val startMap = mapOf(
                com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.TYPE to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.type,
                com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.DATA to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.name
            )
            wsManager.sendSystemMessage(startMap)
        }

        if (audioBuffer.isNotEmpty()) {
            val base64Audio = Base64.encodeToString(audioBuffer, 0, audioBuffer.size, Base64.NO_WRAP)
            val dataMap = mapOf(
                com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.TYPE to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.DATA to base64Audio
            )
            wsManager.sendSystemMessage(dataMap)
        }
    }
}