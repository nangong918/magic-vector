package com.magicvector.utils.chat

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

abstract class AbstractWsClient(
    private val gson : Gson,
    private val baseUrl: String
) {

    companion object {
        const val TAG: String = "WebSocketClient"
        // 减小性能开销，不使用eventbus
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private lateinit var webSocket: WebSocket
    private var lastListener: WebSocketListener? = null

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
        lastListener = listener
        val request = Request.Builder()
            .url(baseUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, listener)
        Log.d(TAG, "start::baseUrl: $baseUrl")
    }

    fun reconnect(listener: WebSocketListener? = null) {
        val actualListener = listener ?: lastListener ?: run {
            Log.w(TAG, "reconnect::listener is null")
            return
        }
        lastListener = actualListener
        close()
        start(actualListener)
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