package com.magicvector.manager.realtime


/**
 * 实时聊天 UI 状态
 */
data class RealtimeChatUiState(
    val isLoading: Boolean = false,
    val agentText: String = ""
)

/**
 * 实时聊天连接状态
 */
open class RealtimeChatState {
    // 未初始化
    object NotInitialized : RealtimeChatState()
    // 正在初始化
    object Initializing : RealtimeChatState()
    // 已初始化并且连接
    object InitializedConnected : RealtimeChatState()
    // 正在记录消息
    object RecordingAndSending : RealtimeChatState()
    // 正在接收消息
    object Receiving : RealtimeChatState()
    // 断开连接
    object Disconnected : RealtimeChatState()
    // 错误
    data class Error(val message: String) : RealtimeChatState()
}

