package com.magicvector.domain.test

open class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    object Streaming : ChatState()
    object Success : ChatState()
    data class Error(val message: String) : ChatState()
}