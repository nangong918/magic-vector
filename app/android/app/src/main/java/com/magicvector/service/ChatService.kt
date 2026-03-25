package com.magicvector.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.magicvector.manager.realtime.RealtimeChatController

/**
 * Chat后台Service
 * 维护RealtimeChatController包括:
 * 1. WS消息长连接
 * 2. VAD语音活动检测（持续录音）
 * 3. YOLOv8物体检测（通过UDP持续发送视频帧）
 * 4. ChatEventManager历史消息
 * @see RealtimeChatController
 */
class ChatService : Service() {

    companion object {
        const val TAG = "ChatService"
    }

    // 聊天资源管理
    private lateinit var realtimeChatController: RealtimeChatController

    override fun onCreate() {
        super.onCreate()
        realtimeChatController = RealtimeChatController()
        Log.i(TAG, "onCreate")
    }

    @Synchronized
    private fun getChatController(): RealtimeChatController {
        return realtimeChatController
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理 startService() 的启动
        Log.i(TAG, "onStartCommand")
        return START_STICKY
    }

    inner class ChatServiceBinder : Binder() {
        fun getChatController(): RealtimeChatController = this@ChatService.getChatController()
    }

    override fun onBind(intent: Intent): IBinder = ChatServiceBinder()

    override fun onDestroy() {
        super.onDestroy()
        realtimeChatController.destroy()
    }
}