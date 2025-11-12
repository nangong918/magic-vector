package com.magicvector.utils.chat

import android.util.Log
import com.google.gson.Gson
import com.magicvector.utils.test.TestRealtimeChatWsClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class RealtimeChatWsClient(
    private val GSON : Gson,
    private val baseUrl: String
) {

    companion object {
        val TAG: String = RealtimeChatWsClient::class.java.name
        // 减小性能开销，不使用eventbus
    }

    private val client = OkHttpClient.Builder()
//        .pingInterval(30, TimeUnit.SECONDS) // 设置心跳间隔
        .build()

    private lateinit var webSocket: WebSocket

    // 发送json数据
    fun sendMessage(messageMap: Map<String, String>){
        val message = GSON.toJson(messageMap)
        webSocket.send(message)
        Log.d(TAG, "send message: ${message.take(50)}")
    }

    fun sendMessage(messageMap: Map<String, String>, isShowAll: Boolean){
        val message = GSON.toJson(messageMap)
        webSocket.send(message)
        if (isShowAll){
            Log.d(TestRealtimeChatWsClient.Companion.TAG, "send message: $message")
        }
        else {
            Log.d(TestRealtimeChatWsClient.Companion.TAG, "send message: ${message.take(50)}")
        }
    }

    // 发送字节数据
    fun sendMessage(message: ByteString){
        webSocket.send(message)
        Log.d(TAG, "send message: ${message.hex().take(50)}")
    }

    fun start(listener: WebSocketListener){
        val request = Request.Builder()
            .url(baseUrl)
            .build()

        webSocket = client.newWebSocket(request, listener)
    }

    fun close() {
        if (::webSocket.isInitialized) {
            webSocket.close(1000, "Bye")
        }
    }
}