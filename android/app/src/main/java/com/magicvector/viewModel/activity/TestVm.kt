package com.magicvector.viewModel.activity

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.test.RealtimeDataTypeEnum
import com.data.domain.event.WebSocketMessageEvent
import com.data.domain.event.WebsocketEventTypeEnum
import com.data.domain.vo.test.AudioRecordPlayState
import com.data.domain.vo.test.ChatState
import com.data.domain.vo.test.RealtimeChatState
import com.data.domain.vo.test.TtsChatState
import com.data.domain.vo.test.WebsocketState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.magicvector.utils.test.SSEClient
import com.magicvector.utils.test.TTS_SSEClient
import com.magicvector.utils.test.TestRealtimeChatWsClient
import com.magicvector.utils.test.TestWebSocketClient
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.math.log
import kotlin.math.sqrt

class TestVm(

) : ViewModel(){

    companion object {
        val TAG: String = TestVm::class.java.name
        val GSON = Gson()
        const val baseSseUrl = BaseConstant.ConstantUrl.LOCAL_URL + "/test/stream-sse"
        const val baseSseTTSUrl = BaseConstant.ConstantUrl.LOCAL_URL + "/test/stream-tts-sse"
        const val baseWebsocketUrl = BaseConstant.ConstantUrl.LOCAL_WS_URL + "/test-channel"
        const val realtimeChatWsUrl = BaseConstant.ConstantUrl.LOCAL_WS_URL + "/realtime-no-vad-test"
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
    val audioRecordVolume = MutableLiveData(0f) // 用于存储音量数据

    // realtime websocket 聊天
    val realtimeChatMessage: MutableLiveData<String> = MutableLiveData("") // 聊天数据，只需要存储text数据，音频数据不要展示是直接播放
    val realtimeChatState: MutableLiveData<RealtimeChatState> = MutableLiveData(RealtimeChatState.NotInitialized)

    //---------------------------NetWork---------------------------

    // websocket realtime聊天
    private var realtimeChatWsClient: TestRealtimeChatWsClient? = null
    var realtimeChatAudioRecord: AudioRecord? = null
    var realtimeChatAudioTrack: AudioTrack? = null
    val realTimeChatSampleRate = 24000
    // 初始化 + 连接
    fun initRealtimeChatWsClient(activity: FragmentActivity) {
        realtimeChatWsClient = TestRealtimeChatWsClient(
            GSON,
            realtimeChatWsUrl
        )

        PermissionUtil.requestPermissionSelectX(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : GainPermissionCallback{
                @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                override fun allGranted() {
                    Log.i(TAG, "获取录音权限成功")

                    initRealtimeChatRecorderAndPlayer()

                    startRealtimeWs()
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    Log.w(TAG, "没有获取录音权限: ${notGrantedPermissions?.contentToString()}")
                    ToastUtils.showToastActivity(activity, "没有获取录音权限")
                    audioRecordPlayState.postValue(AudioRecordPlayState.Error("没有获取录音权限"))
                }

                override fun always() {
                }

            }
        )
    }

    private fun startRealtimeWs() {
        realtimeChatWsClient!!.start(
            object : WebSocketListener() {
                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    super.onClosed(webSocket, code, reason)
                    realtimeChatState.postValue(RealtimeChatState.Disconnected)
                    Log.i(TAG, "realtimeChatWsClient::onClosed")
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    super.onClosing(webSocket, code, reason)
                    Log.i(TAG, "realtimeChatWsClient::onClosing")
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    super.onFailure(webSocket, t, response)
                    Log.e(TAG, "realtimeChatWsClient::onFailure: ${t.message}")
                    realtimeChatState.postValue(RealtimeChatState.Error(t.message ?: "-"))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    // 处理text
                    realtimeChatState.postValue(RealtimeChatState.Receiving)
                    handleTextMessage(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)
                    realtimeChatState.postValue(RealtimeChatState.Receiving)
                    // 处理字节信息
                    Log.i(TAG, "收到字节信息::长度: ${bytes.size}")
                }

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    // 到了此处说明: 授权 && 连接成功
                    realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
                    Log.i(TAG, "realtimeChatWsClient::onOpen; response: $response")
                }
            }
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initRealtimeChatRecorderAndPlayer(){
        // 配置音频参数
        val inChannelConfig = AudioFormat.CHANNEL_IN_MONO
        val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val audioRecordBufferSize = AudioRecord.getMinBufferSize(realTimeChatSampleRate, inChannelConfig, audioFormat)
        val audioTrackBufferSize = AudioTrack.getMinBufferSize(realTimeChatSampleRate, outChannelConfig, audioFormat)

        // 创建AudioRecord
        realtimeChatAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            realTimeChatSampleRate,
            inChannelConfig,
            audioFormat,
            audioRecordBufferSize
        )

        // 创建AudioTrack
        realtimeChatAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            realTimeChatSampleRate,
            outChannelConfig,
            audioFormat,
            audioTrackBufferSize,
            AudioTrack.MODE_STREAM
        )
    }

    private fun handleTextMessage(text: String){
        // text --GSON--> Map<String, String>
        val map: Map<String, String> = GSON.fromJson(text, object : TypeToken<Map<String, String>>() {}.type)

        val typeStr = map[RealtimeDataTypeEnum.TYPE]
        val type = RealtimeDataTypeEnum.getByType(typeStr)

        when(type){
            RealtimeDataTypeEnum.START -> {
                // 开始接收数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                // 清空播放缓存
                recordAudioTrack?.flush()
                // 开始播放
                realtimeChatAudioTrack?.play()
            }
            RealtimeDataTypeEnum.STOP -> {
                // 结束接收数据
                realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
                // 停止播放
                realtimeChatAudioTrack?.stop()
                // 清空播放缓存
                recordAudioTrack?.flush()
            }
            RealtimeDataTypeEnum.AUDIO_CHUNK -> {
                // 音频数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeDataTypeEnum.DATA]
                data?.let {
                    playBase64Audio(data)
                }
            }
            RealtimeDataTypeEnum.TEXT_MESSAGE -> {
                // 文本数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeDataTypeEnum.DATA]
                data?.let {
                    realtimeChatMessage.postValue(realtimeChatMessage.value + data)
                }
            }
        }
    }

    fun playBase64Audio(base64Audio: String) {

        val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)

        // 写入音频数据
        realtimeChatAudioTrack?.write(
            audioBytes,
            0,
            audioBytes.size
        )

        Log.i(TAG, "播放音频数据::: ${audioBytes.take(50)}")
    }

    fun startRecordRealtimeChatAudio() {
        // 录制音频 -> 音频流bytes实时转为Base64的PCM格式 -> 调用websocket的sendAudioMessage
        val bufferSize = AudioRecord.getMinBufferSize(
            realTimeChatSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // 发送启动录音
        val dataMap = mapOf(
            RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.START.type,
            RealtimeDataTypeEnum.DATA to RealtimeDataTypeEnum.START.name
        )
        realtimeChatWsClient?.sendMessage(dataMap)

        // 录制状态
        realtimeChatState.value = RealtimeChatState.RecordingAndSending

        val audioBuffer = ByteArray(bufferSize)
        realtimeChatAudioRecord?.startRecording()

        // 录制音频并转换为 Base64
        Thread {
            while (realtimeChatState.value == RealtimeChatState.RecordingAndSending) {
                val readSize = realtimeChatAudioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    val base64Audio = Base64.encodeToString(audioBuffer, 0, readSize, Base64.NO_WRAP)
                    val dataMap = mapOf(
                        RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.AUDIO_CHUNK.type,
                        RealtimeDataTypeEnum.DATA to base64Audio
                    )
                    realtimeChatWsClient!!.sendMessage(dataMap)
//                    Log.i(TAG, "发送数据:: 类型: ${dataMap[RealtimeDataTypeEnum.TYPE]}; 长度: ${base64Audio.length}; 数据: ${base64Audio.take(100)}")
                }
            }
            realtimeChatAudioRecord?.stop()

            // 发送结束录音
            val dataMap = mapOf(
                RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.STOP.type,
                RealtimeDataTypeEnum.DATA to RealtimeDataTypeEnum.STOP.name
            )
            realtimeChatWsClient!!.sendMessage(dataMap)
//            Log.i(TAG, "发送数据:: 类型: ${dataMap[RealtimeDataTypeEnum.TYPE]}")

        }.start()
    }

    fun stopAndSendRealtimeChatAudio(){
        realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
    }

    fun stopRealtimeChat() {
        realtimeChatAudioRecord?.stop()
        realtimeChatAudioRecord?.release()
        realtimeChatAudioTrack?.stop()
        realtimeChatAudioTrack?.release()
        realtimeChatWsClient?.close()
        realtimeChatState.postValue(RealtimeChatState.Disconnected)
    }

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
    var recordAudioRecord: AudioRecord? = null
    var recordAudioTrack: AudioTrack? = null
    private var recordAudioBuffer: ByteArrayOutputStream? = null

    fun initRecordAudio(activity: FragmentActivity) {
        audioRecordPlayState.value = AudioRecordPlayState.Initializing
        PermissionUtil.requestPermissionSelectX(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                object : GainPermissionCallback{
                    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initMediaRecorder(activity: FragmentActivity) {
        // 配置音频参数
        val sampleRate = 24000
        val inChannelConfig = AudioFormat.CHANNEL_IN_MONO
        val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val audioRecordBufferSize = AudioRecord.getMinBufferSize(sampleRate, inChannelConfig, audioFormat)
        val audioTrackBufferSize = AudioTrack.getMinBufferSize(sampleRate, outChannelConfig, audioFormat)


        // 创建AudioRecord
        recordAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            inChannelConfig,
            audioFormat,
            audioRecordBufferSize
        )

        // 创建AudioTrack
        recordAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            outChannelConfig,
            audioFormat,
            audioTrackBufferSize,
            AudioTrack.MODE_STREAM
        )

        recordAudioBuffer = ByteArrayOutputStream()

        ToastUtils.showToastActivity(activity, "初始化录音成功")
        audioRecordPlayState.value = AudioRecordPlayState.Ready
    }
    // 开始录音
    fun beginRecordAudio(activity: FragmentActivity){

        // 开始录音
        recordAudioRecord?.let {
            it.startRecording()
            audioRecordPlayState.value = AudioRecordPlayState.Recording
        }

        ToastUtils.showToastActivity(activity, "开始录音")

        Thread {
            val buffer = ByteArray(1024)
            val handler = Handler(Looper.getMainLooper())
            val updateInterval = 100L // 100ms

            // 定时更新音量的 Runnable
            val volumeUpdateRunnable = object : Runnable {
                override fun run() {
                    if (audioRecordPlayState.value == AudioRecordPlayState.Recording) {
                        // 使用最近读取的数据计算音量
                        val amplitude = calculateRMSAmplitude(buffer, buffer.size)
                        audioRecordVolume.postValue(amplitude)
                        handler.postDelayed(this, updateInterval)
                    }
                }
            }

            // 启动定时更新
            handler.post(volumeUpdateRunnable)

            while (audioRecordPlayState.value == AudioRecordPlayState.Recording) {
                val read = recordAudioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    // 将录制的数据写入缓存
                    recordAudioBuffer!!.write(buffer, 0, read)
                    // 注意：buffer 会被持续更新，volumeUpdateRunnable 会使用最新的数据
                }
            }

            // 停止录音时移除回调
            handler.removeCallbacks(volumeUpdateRunnable)
        }.start()
    }

    // 计算音量（RMS - 均方根值，更准确）
    // RMS 计算（最准确）
    fun calculateRMSAmplitude(buffer: ByteArray, bytesRead: Int): Float {
        if (bytesRead < 2) return 0f

        var sumSquares = 0.0
        var sampleCount = 0

        for (i in 0 until bytesRead - 1 step 2) {
            // 假设小端序（Android 通常使用）
            val low = buffer[i].toInt() and 0xFF
            val high = buffer[i + 1].toInt() and 0xFF
            val sample = (high shl 8) or low
            val signedSample = if (sample > 32767) sample - 65536 else sample

            sumSquares += signedSample * signedSample
            sampleCount++
        }

        if (sampleCount == 0) return 0f

        val rms = sqrt(sumSquares / sampleCount)
        return minOf(1.0f, (rms / 32767.0).toFloat())
    }

    // 停止录音
    @SuppressLint("DefaultLocale")
    fun stopRecordAudio() {
        recordAudioRecord?.stop()
        recordAudioRecord?.release()

        val audioSize = recordAudioBuffer?.size() ?: 0
        val fileSizeKB = audioSize / 1024.0
        val fileSizeMB = fileSizeKB / 1024.0

        val sampleRate = 24000
        val channelCount = 1
        val bytesPerSample = 2 // 16-bit PCM

        // 计算时长
        val duration = audioSize / (sampleRate * channelCount * bytesPerSample.toDouble())

        var recordMessage = "录音文件信息：\n"

        val formattedFileSizeKB = String.format("%.1f", fileSizeKB)
        val formattedFileSizeMB = String.format("%.1f", fileSizeMB)
        val formattedDuration = String.format("%.1f", duration)

        recordMessage += "文件大小：${formattedFileSizeKB}KB(${formattedFileSizeMB} MB)\n"

        recordMessage += "时长：${formattedDuration}秒\n"

        audioRecordPlayState.value = AudioRecordPlayState.RecordedAndPlayable(recordMessage)
    }

    // 播放录制的音频 (存在问题，不要调用)
    fun playRecordAudio(activity: FragmentActivity){
        if (recordAudioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack 未正确初始化")
            ToastUtils.showToastActivity(activity, "播放器未正确初始化")
            audioRecordPlayState.value = AudioRecordPlayState.Error("播放器未正确初始化")
            return
        }

        recordAudioTrack?.stop()
        recordAudioTrack?.flush() // 清空之前的播放数据

        recordAudioTrack?.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                audioRecordPlayState.value = AudioRecordPlayState.PlayedEnd
                Log.d(TAG, "音频播放完成")
            }

            override fun onPeriodicNotification(track: AudioTrack?) {
            }
        })

        recordAudioTrack?.let {
            it.play()
            audioRecordPlayState.value = AudioRecordPlayState.Playing
        }

        val audioData = recordAudioBuffer!!.toByteArray() // 获取缓存的音频数据
        audioTrack?.write(audioData, 0, audioData.size) // 播放缓存的数据
        Log.d(TAG, "播放缓存的音频数据")
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

