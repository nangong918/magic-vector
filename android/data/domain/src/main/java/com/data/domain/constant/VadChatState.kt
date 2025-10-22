package com.data.domain.constant

open class VadChatState {
    // 静音中
    object Muted : VadChatState()
    // 无声
    object Silent : VadChatState()
    // 用户说话
    object Speaking : VadChatState()
    // Agent回复中
    object Replying : VadChatState()
    // 错误
    data class Error(val message: String) : VadChatState()
}