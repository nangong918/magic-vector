package com.magicvector.viewModel.activity

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioAttributes
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
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
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

        // 创建 AudioTrack（使用 MODE_STREAM 模式）
        recordAudioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(audioFormat)
                .setChannelMask(channelConfig)
                .build(),
            minBufferSize * 2, // 使用两倍缓冲区大小
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        ToastUtils.showToastActivity(activity, "初始化录音成功")
        audioRecordPlayState.value = AudioRecordPlayState.Ready
    }

    private var outputFile: String = ""
    // 开始录音
    fun beginRecordAudio(activity: FragmentActivity){
        // 设置输出文件路径
//        outputFile = "${Environment.getExternalStorageDirectory().absolutePath}/recording.pcm"

        // 使用应用专属目录，不需要存储权限
        outputFile = "${activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath}/recording.pcm"

        // 或者使用内部存储
        // outputFile = "${activity.filesDir.absolutePath}/recording.pcm"

        // 创建文件对象
        val file = File(outputFile)

        // 检查文件夹是否存在，若不存在则创建
        file.parentFile?.let { parentDir ->
            if (!parentDir.exists()) {
                val success = parentDir.mkdirs() // 确保创建目录
                if (!success) {
                    val errorMessage = "Failed to create directory for output file."
                    Log.e(TAG, errorMessage)
                    audioRecordPlayState.value = AudioRecordPlayState.Error(errorMessage)
                    return // 退出方法
                }
            }
        } ?: run {
            val errorMessage = "Parent directory is null."
            audioRecordPlayState.value = AudioRecordPlayState.Error(errorMessage)
            Log.e(TAG, errorMessage)
            return // 退出方法
        }

        // 设置输出文件
        mediaRecorder?.setOutputFile(file.absolutePath) // 设置输出文件路径

        // 准备和启动录音
        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            ToastUtils.showToastActivity(activity, "开始录音")
            audioRecordPlayState.value = AudioRecordPlayState.Recording
        } catch (e: IOException) {
            val errorMessage = "Error starting the MediaRecorder: ${e.message}"
            Log.e(TAG, errorMessage)
            audioRecordPlayState.value = AudioRecordPlayState.Error(errorMessage)
            return // 退出方法
        }
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
    @SuppressLint("DefaultLocale")
    fun stopRecordAudio(activity: FragmentActivity){
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        mediaRecorder?.release()
        ToastUtils.showToastActivity(activity, "停止录音")

        // 先输出文件信息
        val file = File(outputFile)
        var recordMessage = "录音文件信息：\n"

        if (!file.exists()) {
            val errorMessage = "录音文件不存在: $outputFile"
            Log.e(TAG, errorMessage)
            audioRecordPlayState.value = AudioRecordPlayState.Error(errorMessage)
            return
        }

        val fileSize = file.length()
        val fileSizeKB = fileSize / 1024.0
        val fileSizeMB = fileSizeKB / 1024.0

        // 估算时长（AAC格式，单声道，16kHz采样率）
        // 根据 AAC 格式和录音参数估算时长
        // AAC 通常的比特率：64 kbps 到 192 kbps，这里取常用值 128 kbps
        val bitrate = 128000 // 128 kbps in bits per second

        // 文件大小是字节，比特率是比特/秒，所以需要转换
        // 时长(秒) = (文件大小(字节) * 8) / 比特率(比特/秒)
        val duration = (fileSize * 8.0) / bitrate

        // 保留一位小数
        val formattedFileSizeKB = String.format("%.1f", fileSizeKB)
        val formattedFileSizeMB = String.format("%.1f", fileSizeMB)
        val formattedDuration = String.format("%.1f", duration)

        recordMessage += "文件大小：${formattedFileSizeKB}（${formattedFileSizeMB} MB）：\n"

        recordMessage += "时长：${formattedDuration}秒\n"

        audioRecordPlayState.value = AudioRecordPlayState.RecordedAndPlayable(recordMessage)
    }

    // 播放录制的音频
    fun playRecordAudio(activity: FragmentActivity){
        if (recordAudioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack 未正确初始化")
            ToastUtils.showToastActivity(activity, "播放器未正确初始化")
            audioRecordPlayState.value = AudioRecordPlayState.Error("播放器未正确初始化")
            return
        }

        // 检查文件是否存在
        val file = File(outputFile)
        if (!file.exists()) {
            Log.e(TAG, "录音文件不存在: $outputFile")
            ToastUtils.showToastActivity(activity, "录音文件不存在")
            audioRecordPlayState.value = AudioRecordPlayState.Error("录音文件不存在")
            return
        }

        try {
            val fis = FileInputStream(file)
            val audioData = ByteArray(4096) // 定义缓冲区大小

            // 先停止之前的播放（如果有）
            recordAudioTrack?.stop()
            recordAudioTrack?.flush()

            // 开始播放
            recordAudioTrack?.let {
                it.play()
                audioRecordPlayState.value = AudioRecordPlayState.Playing
            }

            var bytesRead: Int
            // 逐块读取文件数据并播放
            while (fis.read(audioData).also { bytesRead = it } > 0) {
                var totalWritten = 0
                while (totalWritten < bytesRead) {
                    val written = recordAudioTrack?.write(audioData, totalWritten, bytesRead - totalWritten) ?: 0
                    if (written <= 0) {
                        break
                    }
                    totalWritten += written
                }
            }

            // 等待播放完成
            recordAudioTrack?.stop()
            audioRecordPlayState.value = AudioRecordPlayState.PlayedEnd
            fis.close()

            Log.i(TAG, "录音播放完成")
            ToastUtils.showToastActivity(activity, "录音播放完成")

        } catch (e: IOException) {
            Log.e(TAG, "播放录音失败", e)
            ToastUtils.showToastActivity(activity, "播放录音失败: ${e.message}")
            audioRecordPlayState.value = AudioRecordPlayState.Error("播放录音失败: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "AudioTrack 状态异常", e)
            ToastUtils.showToastActivity(activity, "播放器状态异常: ${e.message}")
            audioRecordPlayState.value = AudioRecordPlayState.Error("播放器状态异常: ${e.message}")
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