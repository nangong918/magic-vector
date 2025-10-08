package com.magicvector.viewModel.activity

import android.Manifest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.constant.BaseConstant
import com.data.domain.event.WebSocketMessageEvent
import com.data.domain.event.WebsocketEventTypeEnum
import com.data.domain.vo.test.AudioRecordPlayState
import com.data.domain.vo.test.ChatState
import com.data.domain.vo.test.TtsChatState
import com.data.domain.vo.test.WebsocketState
import com.google.gson.Gson
import com.magicvector.utils.test.SSEClient
import com.magicvector.utils.test.TTS_SSEClient
import com.magicvector.utils.test.TestWebSocketClient
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.IOException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class TestVm(

) : ViewModel(){

    companion object {
        val TAG: String = TestVm::class.java.name
        val GSON = Gson()
        const val baseSseUrl = BaseConstant.ConstantUrl.LOCAL_URL + "/test/stream-sse"
        const val baseSseTTSUrl = BaseConstant.ConstantUrl.LOCAL_URL + "/test/stream-tts-sse"
        const val baseWebsocketUrl = BaseConstant.ConstantUrl.LOCAL_WS_URL + "/test-channel"
    }

    init {
        // 测试的时候手动点击初始化
//        initializeAudioTrack()
        Log.i(TAG, "init finish")
    }

    //---------------------------AAo Ld---------------------------

    private val customQuestion = "你好啊，你是谁？介绍一下自己吧！"

    //==========UI State
    val sseChatMessage: MutableLiveData<String> = MutableLiveData("")
    val sseChatState: MutableLiveData<ChatState> = MutableLiveData(ChatState.Idle)

    val ttsSseChatMessage: MutableLiveData<String> = MutableLiveData("")
    val ttsSseChatState: MutableLiveData<TtsChatState> = MutableLiveData(TtsChatState.NotInitialized)

    // websocket (逆天bug，都出现了量子力学的观察者效应，观察就没bug，不观察就有bug，简直逆天；已经排除了build和混淆)
    val websocketAllMessage: MutableLiveData<String> = MutableLiveData("")
    val websocketState: MutableLiveData<WebsocketState> = MutableLiveData(WebsocketState.NotInitialized)

    val audioRecordPlayState: MutableLiveData<AudioRecordPlayState> = MutableLiveData(AudioRecordPlayState.NotInitialized)

    //---------------------------NetWork---------------------------

    // sse
    private val sseClient = SSEClient(
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // 设置读取超时时间为0，表示无限读取
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        ,
        GSON,
        baseSseUrl
    )

    fun sendQuestion() {
        viewModelScope.launch {
            sseChatState.value = ChatState.Loading
            sseChatMessage.value = ""

            sseClient.streamChat(customQuestion)
                .collect { data ->
                    sseChatMessage.value += data
                    sseChatState.value = ChatState.Streaming
                    Log.i(TAG, "收到消息: $data")
                }

            sseChatState.value = ChatState.Success
        }
    }

    // tts sse
    private val ttsSSEClient = TTS_SSEClient(
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // 读取超时设为0，表示无限读取
            .writeTimeout(10, TimeUnit.SECONDS)
            .build(),
        GSON,
        baseSseTTSUrl
    )

    fun sendTTSQuestion() {
        viewModelScope.launch {
            ttsSseChatState.value = TtsChatState.Loading
            ttsSseChatMessage.value = ""

            ttsSSEClient.streamTTSChat(customQuestion)
                .collect { dataMap ->
                    when (dataMap["type"]?:"error") {
                        "text" -> {
                            // 处理文本数据，用于显示
                            val text = dataMap["data"] ?: ""
                            ttsSseChatMessage.value += text
                            ttsSseChatState.value = TtsChatState.Streaming
                            Log.i(TAG, "收到消息: $text")
                        }
                        "audio" -> {
                            // 处理音频数据，用于播放
                            val base64Audio = dataMap["data"] ?: ""
                            ttsPlayAudio(base64Audio)
                        }
                    }
                }

            ttsSseChatState.value = TtsChatState.Success
        }
    }

    private var testWebSocketClient: TestWebSocketClient? = null

    // websocket
    fun connectWebsocket(){
        websocketState.value = WebsocketState.Initializing

        initWebsocketEventBus()

        testWebSocketClient = TestWebSocketClient(
            GSON,
            baseWebsocketUrl
        )

        websocketState.value = WebsocketState.InitializedNotConnected

        testWebSocketClient?.start()
    }

    fun sendWebsocketMessage(message: String){
        websocketState.value = WebsocketState.Sending
        testWebSocketClient?.sendMessage(message)
    }

    fun disconnectWebsocket(){
        testWebSocketClient?.close()
        websocketState.value = WebsocketState.Disconnected
    }

    //---------------------------Logic---------------------------

    // 录音
    var mediaRecorder: MediaRecorder? = null
    var recordAudioTrack: AudioTrack? = null

    fun initRecordAudio(activity: FragmentActivity) {
        audioRecordPlayState.value = AudioRecordPlayState.Initializing
        PermissionUtil.requestPermissionSelectX(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            arrayOf(),
                object : GainPermissionCallback{
                    override fun allGranted() {
                        Log.i(TAG, "获取录音权限成功")
                        initMediaRecorder(activity)
                    }

                    override fun notGranted(notGrantedPermissions: Array<String?>?) {
                        Log.w(TAG, "没有获取录音权限: ${notGrantedPermissions?.contentToString()}")
                        ToastUtils.showToastActivity(activity, "没有获取录音权限")
                        audioRecordPlayState.value = AudioRecordPlayState.Error("没有获取录音权限")
                    }

                    override fun always() {
                    }

                }
        )
    }

    private fun initMediaRecorder(activity: FragmentActivity){
        // 初始化录音器
        mediaRecorder = MediaRecorder().apply {
            // 设置麦克风
            setAudioSource(MediaRecorder.AudioSource.MIC)
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }
        // 初始化播放器
        val sampleRate = 24000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        // 计算最小缓冲区大小
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        // 创建 AudioTrack（使用模式为 MODE_STATIC）
        recordAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize,
            AudioTrack.MODE_STATIC
        )
        ToastUtils.showToastActivity(activity, "初始化录音成功")
        audioRecordPlayState.value = AudioRecordPlayState.Ready
    }

    private var outputFile: String = ""
    // 开始录音
    fun beginRecordAudio(activity: FragmentActivity){
        // 设置输出文件路径
        outputFile = "${Environment.getExternalStorageDirectory().absolutePath}/recording.pcm"

        // 文件名称的文件夹不存在就创建文件夹
        val file = File(outputFile)
        file.parentFile?.exists()?.let {
            if (!it) {
                file.parentFile?.mkdirs()
            }
        }

        mediaRecorder?.prepare()
        mediaRecorder?.start()

        ToastUtils.showToastActivity(activity, "开始录音")
        audioRecordPlayState.value = AudioRecordPlayState.Recording
    }

    // 暂停录音
    fun pauseRecordAudio(activity: FragmentActivity){
        mediaRecorder?.pause()
        ToastUtils.showToastActivity(activity, "暂停录音")

        audioRecordPlayState.value = AudioRecordPlayState.Paused
    }

    // 录音继续
    fun resumeRecordAudio(activity: FragmentActivity){
        mediaRecorder?.resume()
        ToastUtils.showToastActivity(activity, "继续录音")

        audioRecordPlayState.value = AudioRecordPlayState.Recording
    }

    // 停止录音
    fun stopRecordAudio(activity: FragmentActivity){
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        mediaRecorder?.release()
        ToastUtils.showToastActivity(activity, "停止录音")

        audioRecordPlayState.value = AudioRecordPlayState.RecordedAndPlayable
    }

    // 播放录制的音频
    fun playRecordAudio(activity: FragmentActivity){
        // 读取文件并播放
        val file = File(outputFile)
        val fis = FileInputStream(file)
        try {

            val audioData = ByteArray(4096) // 定义缓冲区大小

            recordAudioTrack?.play() // 开始播放
            var bytesRead: Int

            audioRecordPlayState.value = AudioRecordPlayState.Playing

            // 逐块读取文件数据并播放
            while (fis.read(audioData).also { bytesRead = it } > 0) {
                recordAudioTrack?.write(audioData, 0, bytesRead)
            }

            audioRecordPlayState.value = AudioRecordPlayState.PlayedEnd
        } catch (e: IOException) {
            Log.e(TAG, "播放录音失败", e)
            ToastUtils.showToastActivity(activity, "播放录音失败: ${e.message}")
            audioRecordPlayState.value = AudioRecordPlayState.Error("播放录音失败: ${e.message}")
        } finally {
            fis.close()
        }
    }

    // tts 播放音频
    private fun ttsPlayAudio(base64Data: String) {
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

    // 初始化AudioTrack
    fun initializeAudioTrack() {
        when (ttsSseChatState.value) {
            is TtsChatState.NotInitialized,
            is TtsChatState.InitializationFailed -> {
                // 需要进行初始化
            }
            else -> {
                // 正在、已经初始化了就回滚
                return
            }
        }

        try {
            ttsSseChatState.value = TtsChatState.Initializing
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

            ttsSseChatState.value = TtsChatState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack初始化失败", e)
            ttsSseChatState.value = TtsChatState.InitializationFailed(e.message ?: "未知错误")
        }
    }

    private var isWebsocketEventBusInitialized = false

    // 初始化websocket的eventbus
    private fun initWebsocketEventBus() {
        if (isWebsocketEventBusInitialized) {
            return
        }
        EventBus.getDefault().register(this)
        isWebsocketEventBusInitialized = true
    }

    // 订阅
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWebSocketMessageEvent(event: WebSocketMessageEvent) {
        // 处理接收到的 WebSocket 消息
        when (event.eventType) {
            WebsocketEventTypeEnum.ON_OPEN -> {
                websocketState.postValue(WebsocketState.Connected)
                websocketAllMessage.postValue(websocketAllMessage.value + "\n[${event.eventType.desc}]: ${event.text}")
            }
            WebsocketEventTypeEnum.ON_MESSAGE -> {
                websocketState.postValue(WebsocketState.Receiving)
                websocketAllMessage.postValue(websocketAllMessage.value + "\n[${event.eventType.desc}]: ${event.text}")
            }
            WebsocketEventTypeEnum.ON_CLOSING -> {
                websocketState.postValue(WebsocketState.Disconnected)
                websocketAllMessage.postValue(websocketAllMessage.value + "\n[${event.eventType.desc}]: ${event.text}")
            }
            WebsocketEventTypeEnum.ON_FAILURE -> {
                websocketState.postValue(WebsocketState.Error(event.text))
                websocketAllMessage.postValue(websocketAllMessage.value + "\n[${event.eventType.desc}]: ${event.text}")
            }
            WebsocketEventTypeEnum.ON_MESSAGE_BYTE -> {
                websocketState.postValue(WebsocketState.Receiving)
                websocketAllMessage.postValue(websocketAllMessage.value + "\n[${event.eventType.desc}]: ${event.text}")
            }
            WebsocketEventTypeEnum.ON_CLOSED -> {
                websocketState.postValue(WebsocketState.Disconnected)
                websocketAllMessage.postValue(websocketAllMessage.value + "\n[${event.eventType.desc}]: ${event.text}")
            }
            WebsocketEventTypeEnum.SEND_MESSAGE -> {
                websocketState.postValue(WebsocketState.Sending)
                websocketAllMessage.postValue(websocketAllMessage.value + "\n[${event.eventType.desc}]: ${event.text}")
            }
        }
        Log.i(TAG, "onWebSocketMessageEvent::websocketState: ${websocketState.value}")
        Log.i(TAG, "onWebSocketMessageEvent::websocketAllMessage: ${websocketAllMessage.value}")
    }

    // 销毁
    private fun unregisterWebsocketEventBus() {
        EventBus.getDefault().unregister(this)
        isWebsocketEventBusInitialized = false
    }

    // 在适当的时候释放资源
    fun releaseAudioResources() {
        audioTrack?.let {
            it.stop()
            it.release()
            audioTrack = null
            Log.d(TAG, "AudioTrack资源已释放")
        }
        ttsSseChatState.value = TtsChatState.NotInitialized
    }

    override fun onCleared() {
        super.onCleared()
        releaseAudioResources()
        unregisterWebsocketEventBus()
        Log.i(TAG, "onCleared")
    }

}