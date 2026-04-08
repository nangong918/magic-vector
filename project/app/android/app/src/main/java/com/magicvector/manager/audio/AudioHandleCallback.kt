package com.magicvector.manager.audio

/**
 * 处理音频的回调
 */
interface AudioHandleCallback {
    /**
     * 播放base64音频
     * @param base64Audio base64音频
     */
    fun onPlayBase64Audio(base64Audio: String)

    /**
     * 开始录音
     */
    fun onStartRecording()
    /**
     * 获取录音音频
     * @param base64Audio 音频
     */
    fun onObtainAudio(base64Audio: String)
    /**
     * 停止录音
     */
    fun onStopRecording()
}