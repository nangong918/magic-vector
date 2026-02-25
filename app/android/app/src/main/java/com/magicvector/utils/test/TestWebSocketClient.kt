package com.magicvector.utils.test

import android.util.Log
import com.data.domain.event.WebSocketMessageEvent
import com.data.domain.event.WebsocketEventTypeEnum
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class TestWebSocketClient(
    private val GSON : Gson,
    private val baseUrl: String
) {

    companion object {
        val TAG: String = TestWebSocketClient::class.java.name
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS) // 设置心跳间隔
        .build()

    private lateinit var webSocket: WebSocket

    fun sendMessage(message: String) {
        if (::webSocket.isInitialized) {
            webSocket.send(message)
            EventBus.getDefault().post(WebSocketMessageEvent(message,
                WebsocketEventTypeEnum.SEND_MESSAGE))
        }
    }

    fun start() {
        val request = Request.Builder()
            .url(baseUrl) // 替换为你的服务器地址
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket opened: ${response.message}")
                EventBus.getDefault().post(WebSocketMessageEvent(response.message,
                    WebsocketEventTypeEnum.ON_OPEN))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.i(TAG,"Message received: $text")
                EventBus.getDefault().post(WebSocketMessageEvent(text,
                    WebsocketEventTypeEnum.ON_MESSAGE))
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.i(TAG,"Message received: ${bytes.hex()}")
                EventBus.getDefault().post(WebSocketMessageEvent(bytes.hex(),
                    WebsocketEventTypeEnum.ON_MESSAGE_BYTE))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.i(TAG,"WebSocket closing: code=$code, reason=$reason")
                EventBus.getDefault().post(WebSocketMessageEvent(reason,
                    WebsocketEventTypeEnum.ON_CLOSING))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.i(TAG,"WebSocket error: ${t.message}")
                EventBus.getDefault().post(WebSocketMessageEvent(t.message ?: "未知错误",
                    WebsocketEventTypeEnum.ON_FAILURE))
            }
        })

        // 发送一条消息
        sendMessage("Hello, Server!")
    }

    fun close() {
        if (::webSocket.isInitialized) {
            webSocket.close(1000, "Bye")
            EventBus.getDefault().post(WebSocketMessageEvent("Bye",
                WebsocketEventTypeEnum.ON_CLOSED))
        }
    }
}