package com.data.domain.vo.test

open class TtsChatState {
    object NotInitialized : TtsChatState()
    // 正在初始化
    object Initializing : TtsChatState()
    // 初始化失败
    data class InitializationFailed(val message: String) : TtsChatState()
    object Idle : TtsChatState()
    object Loading : TtsChatState()
    object Streaming : TtsChatState()
    object Success : TtsChatState()
    data class Error(val message: String) : TtsChatState()
}