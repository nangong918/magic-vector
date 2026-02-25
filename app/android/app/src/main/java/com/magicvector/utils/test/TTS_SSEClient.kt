package com.magicvector.utils.test

import android.util.Log
import com.data.domain.dto.request.ChatRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class TTS_SSEClient (
    private val okHttpClient: OkHttpClient,
    private val GSON : Gson,
    private val baseUrl: String
) {

    companion object {
        val TAG: String = SSEClient::class.java.name
    }
    
    fun streamTTSChat(question: String): Flow<Map<String, String>> = callbackFlow {
        val chatRequest = ChatRequest()
        chatRequest.question = question

        chatRequest.question = question
        val requestBody = GSON.toJson(chatRequest)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .build()

        Log.i(TAG, "🔗 开始TTS SSE连接: $baseUrl")
        Log.i(TAG, "📤 发送TTS请求: $question")


        val eventSourceFactory = EventSources.createFactory(okHttpClient)

        val eventSource = eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // 连接打开
                Log.i(TAG, "✅ TTS SSE连接已打开")
                Log.i(TAG, "📋 响应码: ${response.code}")
                Log.i(TAG, "📋 Content-Type: ${response.header("Content-Type")}")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.i(TAG, "📥 收到TTS SSE事件 - id: $id, type: $type")
                try {
                    if (data.isNotEmpty()) {
                        // 解析JSON数据为Map<String, String>
                        val dataMap = parseEventData(data)
                        Log.i(TAG, "📥 解析后的数据 - type: ${dataMap["type"]}, data长度: ${dataMap["data"]?.length ?: 0}")

                        // 根据不同类型进行日志记录
                        when (dataMap["type"]?:"error") {
                            "text" -> {
                                Log.i(TAG, "📝 收到文本: ${dataMap["data"]}")
                            }
                            "audio" -> {
                                Log.i(TAG, "🎵 收到音频数据，Base64长度: ${dataMap["data"]?.length ?: 0}")
                            }
                            "paragraph_start" -> {
                                Log.i(TAG, "📖 段落开始: ${dataMap["data"]}")
                            }
                            "end" -> {
                                Log.i(TAG, "🏁 流结束: ${dataMap["data"]}")
                            }
                            "error" -> {
                                Log.e(TAG, "❌ 错误信息: ${dataMap["data"]}")
                            }
                        }

                        trySend(dataMap) // 发送Map数据到Flow
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 解析SSE数据失败: ${e.message}", e)
                    // 发送错误信息
                    val errorMap = mapOf(
                        "type" to "error",
                        "data" to "数据解析失败: ${e.message}",
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                    trySend(errorMap)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.i(TAG, "🔚 TTS SSE连接正常关闭")
                close() // 关闭Flow
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = buildString {
                    append("❌ TTS SSE连接失败\n")
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
                            append("响应体: 无法读取, error: ${e.message}\n")
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

    /**
     * 解析事件数据为Map<String, String>
     */
    open fun parseEventData(jsonData: String): Map<String, String> {
        return try {
            // 直接使用GSON解析为Map<String, String>
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            GSON.fromJson<Map<String, String>>(jsonData, mapType) ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "解析JSON失败: $jsonData", e)
            mapOf(
                "type" to "error",
                "data" to "无效的JSON数据: ${e.message}"
            )
        }
    }

}