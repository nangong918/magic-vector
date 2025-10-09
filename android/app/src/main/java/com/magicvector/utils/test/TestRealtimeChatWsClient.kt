package com.magicvector.utils.test

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class TestRealtimeChatWsClient(
    private val GSON : Gson,
    private val baseUrl: String
) {

    companion object {
        val TAG: String = TestRealtimeChatWsClient::class.java.name
        // 减小性能开销，不使用eventbus
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS) // 设置心跳间隔
        .build()

    private lateinit var webSocket: WebSocket

    fun sendMessage(messageMap: Map<String, String>){
        if (::webSocket.isInitialized) {
            webSocket.send(GSON.toJson(messageMap))
        }
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