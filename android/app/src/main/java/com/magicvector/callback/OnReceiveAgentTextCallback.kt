package com.magicvector.callback

/**
 * 当接收到后端Text的回调
 */
interface OnReceiveAgentTextCallback {
    fun onText(text: String)
}