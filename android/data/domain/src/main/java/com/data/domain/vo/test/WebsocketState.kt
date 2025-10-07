package com.data.domain.vo.test

open class WebsocketState {
    // 未初始化
    object NotInitialized : WebsocketState()
    // 正在初始化
    object Initializing : WebsocketState()
    // 初始化失败
    data class InitializationFailed(val message: String) : WebsocketState()
    // 已初始化未连接
    object InitializedNotConnected : WebsocketState()
    // 已连接
    object Connected : WebsocketState()
    // 正在发送消息
    object Sending : WebsocketState()
    // 正在接收消息
    object Receiving : WebsocketState()
    // 断开连接
    object Disconnected : WebsocketState()
    // 完成
    object Completed : WebsocketState()
    // 错误
    data class Error(val message: String) : WebsocketState()
}