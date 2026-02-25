package com.magicvector.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.magicvector.manager.RealtimeChatController
import com.magicvector.manager.yolo.VisionManager

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
    private var realtimeChatController: RealtimeChatController? = null

    override fun onCreate() {
        super.onCreate()
        // 可以在这里进行一些初始化
        realtimeChatController = RealtimeChatController()
    }

    private fun getChatHandler(): RealtimeChatController {
        if (realtimeChatController == null) {
            realtimeChatController = RealtimeChatController()
        }
        return realtimeChatController!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理 startService() 的启动
        return START_STICKY
    }

    inner class ChatServiceBinder : Binder() {
        fun getChatMessageHandler(): RealtimeChatController = getChatHandler()
        fun getService(): ChatService = this@ChatService
    }

    override fun onBind(intent: Intent): IBinder {
        return ChatServiceBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        realtimeChatController?.destroy()
        realtimeChatController = null
    }
}