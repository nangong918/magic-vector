package com.magicvector.viewModel.activity

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
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

    init {
        initializeAudioTrack()
        Log.i(TAG, "init finish")
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
            // 解码Base64数据
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            Log.d(TAG, "解码音频数据，长度: ${audioBytes.size} 字节")

            // 如果AudioTrack未初始化，先初始化
            if (audioTrack == null) {
                initializeAudioTrack()
            }

            // 写入音频数据
            audioTrack?.write(audioBytes, 0, audioBytes.size)
            Log.d(TAG, "音频数据已写入AudioTrack")
        } catch (e: Exception) {
            Log.e(TAG, "播放音频失败", e)
        }
    }

    private var audioTrack: AudioTrack? = null

    private fun initializeAudioTrack() {
        try {
            // 配置音频参数（与后端保持一致）
            val sampleRate = 24000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            // 计算最小缓冲区大小
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            // 创建AudioTrack（使用流模式）
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize,
                AudioTrack.MODE_STREAM
            )

            // 开始播放
            audioTrack?.play()
            Log.d(TAG, "AudioTrack初始化完成，缓冲区大小: $minBufferSize")

        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack初始化失败", e)
        }
    }

    // 在适当的时候释放资源
    fun releaseAudioResources() {
        audioTrack?.let {
            it.stop()
            it.release()
            audioTrack = null
            Log.d(TAG, "AudioTrack资源已释放")
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseAudioResources()
        Log.i(TAG, "onCleared")
    }

}