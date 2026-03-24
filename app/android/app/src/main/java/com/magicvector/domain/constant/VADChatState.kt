package com.magicvector.domain.constant

open class VADChatState {
    // 静音中
    object Muted : VADChatState()
    // 无声音
    object Silent : VADChatState()
    // 用户说话
    object Speaking : VADChatState()
    // Agent回复中
    object Replying : VADChatState()
    // 错误
    data class Error(val message: String) : VADChatState()
}