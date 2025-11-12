package com.data.domain.vo.test

open class AudioRecordPlayState {
    // 未初始化
    object NotInitialized : AudioRecordPlayState()
    // 正在初始化
    object Initializing : AudioRecordPlayState()
    // 就绪
    object Ready : AudioRecordPlayState()
    // 正在录音
    object Recording : AudioRecordPlayState()
    // 录音结束，可播放
    data class RecordedAndPlayable(val recordMessage: String) : AudioRecordPlayState()
    // 正在播放
    object Playing : AudioRecordPlayState()
    // 播放结束
    object PlayedEnd : AudioRecordPlayState()
    // 错误
    data class Error(val message: String) : AudioRecordPlayState()
}