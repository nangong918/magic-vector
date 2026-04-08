package com.magicvector.utils.test

import android.util.Log
import com.magicvector.domain.dto.http.request.ChatRequest
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import okhttp3.sse.EventSourceListener

class SSEClient(
    private val okHttpClient: OkHttpClient,
    private val GSON : Gson,
    private val baseUrl: String
) {

    companion object {
        val TAG: String = SSEClient::class.java.name
    }

    fun streamChat(question: String): Flow<String> = callbackFlow {
        val chatRequest = ChatRequest()
        chatRequest.question = question
        val requestBody = GSON.toJson(chatRequest)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .build()

        Log.i(TAG, "🔗 开始SSE连接: $baseUrl")
        Log.i(TAG,"📤 发送问题: $question")

        val eventSourceFactory = EventSources.createFactory(okHttpClient)

        val eventSource = eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // 连接打开
                Log.i(TAG,"✅ SSE连接已打开")
                Log.i(TAG,"📋 响应码: ${response.code}")
                Log.i(TAG,"📋 响应头: ${response.headers}")
                Log.i(TAG,"📋 Content-Type: ${response.header("Content-Type")}")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.i(TAG,"📥 收到SSE事件 - id: $id, type: $type")
                Log.i(TAG,"📥 数据内容: $data")
                trySend(data) // 发送数据到 Flow
            }

            override fun onClosed(eventSource: EventSource) {
                Log.i(TAG,"🔚 SSE连接正常关闭")
                close() // 关闭 Flow
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = buildString {
                    append("❌ SSE连接失败\n")
                        .append("请求url: $baseUrl\n")
                    t?.let { append("异常: ${it.message}\n") }
                    response?.let {
                        append("响应码: ${it.code}\n")
                        append("响应消息: ${it.message}\n")
                        append("响应头: ${it.headers}\n")
                        try {
                            val bodyString = it.body.string()
                            append("响应体: $bodyString\n")
                        } catch (e: Exception) {
                            append("响应体: 无法读取\n")
                        }
                    }
                }
                Log.i(TAG,errorMsg)
                close(Exception(errorMsg))
            }
        })

        awaitClose {
            Log.i(TAG,"🛑 取消SSE连接")
            eventSource.cancel() // 当 Flow 被取消时关闭 EventSource
        }
    }

}