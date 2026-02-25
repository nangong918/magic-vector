package com.magicvector.utils.chat

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

abstract class AbstractWsClient(
    private val gson : Gson,
    private val baseUrl: String
) {

    companion object {
        const val TAG: String = "WebSocketClient"
        // 减小性能开销，不使用eventbus
    }

    private lateinit var webSocket: WebSocket

    fun sendMessage(messageMap: Map<String, String>){
        val message = gson.toJson(messageMap)
        webSocket.send(message)
    }
    fun sendMessage(messageMap: Map<String, String>, isShowAllLog: Boolean){
        val message = gson.toJson(messageMap)
        webSocket.send(message)
        if (isShowAllLog) {
            Log.d(TAG, "send message: $message")
        }
        else {
            Log.d(TAG, "send message: ${message.take(50)}")
        }
    }
    fun sendMessage(message: ByteString){
        webSocket.send(message)
        Log.d(TAG, "send message: ${message.hex().take(50)}")
    }

    fun start(listener: WebSocketListener){
        val client = OkHttpClient.Builder()
//            .pingInterval(30, TimeUnit.SECONDS) // 设置心跳间隔
            .build()

        val request = Request.Builder()
            .url(baseUrl)
            .build()

        webSocket = client.newWebSocket(request, listener)
        Log.d(TAG, "start::baseUrl: $baseUrl")
    }
    fun close(){
        if (::webSocket.isInitialized) {
            webSocket.close(1000, "Bye")
        }
        else {
            Log.d(TAG, "close::webSocket is not initialized")
        }
    }
    fun getBaseUrl(): String{
        return baseUrl
    }

}