package com.magicvector.viewModel.activity

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.constant.BaseConstant
import com.data.domain.vo.test.ChatState
import com.google.gson.Gson
import com.magicvector.utils.test.SSEClient
import com.magicvector.utils.test.TTS_SSEClient
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class TestVm(

) : ViewModel(){

    companion object {
        val TAG: String = TestVm::class.java.name
        val GSON = Gson()
        const val baseUrl = BaseConstant.ConstantUrl.LOCAL_URL + "/test/stream-sse"
        const val baseTTSUrl = BaseConstant.ConstantUrl.LOCAL_URL + "/test/stream-tts-sse"
    }

    //---------------------------AAo Ld---------------------------

    private val customQuestion = "你好啊，你是谁？介绍一下自己吧！"

    //---------------------------UI State---------------------------
    val currentMessage: MutableLiveData<String> = MutableLiveData("")
    val chatState: MutableLiveData<ChatState> = MutableLiveData(ChatState.Idle)

    //---------------------------NetWork---------------------------

    private val sseClient = SSEClient(
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // 设置读取超时时间为0，表示无限读取
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        ,
        GSON,
        baseUrl
    )

    fun sendQuestion() {
        viewModelScope.launch {
            chatState.value = ChatState.Loading
            currentMessage.value = ""

            sseClient.streamChat(customQuestion)
                .collect { data ->
                    currentMessage.value += data
                    chatState.value = ChatState.Streaming
                    Log.i(TAG, "收到消息: $data")
                }

            chatState.value = ChatState.Success
        }
    }

    private val ttsSSEClient = TTS_SSEClient(
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // 读取超时设为0，表示无限读取
            .writeTimeout(10, TimeUnit.SECONDS)
            .build(),
        GSON,
        baseTTSUrl
    )

    fun sendTTSQuestion() {
        viewModelScope.launch {
            chatState.value = ChatState.Loading
            currentMessage.value = ""

            ttsSSEClient.streamTTSChat(customQuestion)
                .collect { dataMap ->
                    when (dataMap["type"]?:"error") {
                        "text" -> {
                            // 处理文本数据，用于显示
                            val text = dataMap["data"] ?: ""
                            currentMessage.value += text
                            chatState.value = ChatState.Streaming
                        }
                        "audio" -> {
                            // 处理音频数据，用于播放
                            val base64Audio = dataMap["data"] ?: ""
                            playAudio(base64Audio)
                        }
                    }
                }

            chatState.value = ChatState.Success
        }
    }

    //---------------------------Logic---------------------------

    private fun playAudio(base64Data: String) {
        // 在这里实现音频播放逻辑
        // 例如使用MediaPlayer播放Base64音频
        try {
            // 解码Base64并播放
            // val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            // ... 播放逻辑
            Log.d(TAG, "播放音频数据")
        } catch (e: Exception) {
            Log.e(TAG, "播放音频失败", e)
        }
    }

}