package com.magicvector.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.magicvector.manager.ChatMessageHandler

/**
 * Chat后台Service
 * Chat功能: 公用一个websocket，状态值控制
 * Text Chat
 * ImageText Chat
 * Call Chat
 * Video Chat
 * 页面:
 * MessageFragment（MainActivity）
 * ChatActivity
 * AgentEmojiActivity
 * Manager:
 * ChatManager
 */
class ChatService : Service() {

    // 聊天资源管理
    private var chatMessageHandler: ChatMessageHandler? = null

    override fun onCreate() {
        super.onCreate()
        // 可以在这里进行一些初始化
        chatMessageHandler = ChatMessageHandler()
    }

    fun getChatHandler(): ChatMessageHandler {
        if (chatMessageHandler == null) {
            chatMessageHandler = ChatMessageHandler()
        }
        return chatMessageHandler!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理 startService() 的启动
        return START_STICKY
    }

    inner class ChatServiceBinder : Binder() {
        fun getChatMessageHandler(): ChatMessageHandler = getChatHandler()
    }

    override fun onBind(intent: Intent): IBinder {
        return ChatServiceBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatMessageHandler?.destroy()
        chatMessageHandler = null
    }
}