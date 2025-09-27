package com.view.appview.chat

interface OnChatMessageClick {
    // 点击消息
    fun onMessageClick(position: Int)
    // 点击头像
    fun onAvatarClick(position: Int)
    // 引用消息
    fun onQuoteClick(position: Int)
}