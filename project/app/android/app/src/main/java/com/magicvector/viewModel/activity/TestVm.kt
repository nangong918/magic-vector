package com.magicvector.viewModel.activity

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.domain.constant.BaseConstant
import com.magicvector.domain.constant.test.RealtimeDataTypeEnum
import com.magicvector.domain.dto.ws.request.RealtimeChatConnectRequest
import com.magicvector.domain.dto.ws.response.WsChatTextResponse
import com.magicvector.domain.event.WebSocketMessageEvent
import com.magicvector.domain.event.WebsocketEventTypeEnum
import com.magicvector.domain.test.AudioRecordPlayState
import com.magicvector.domain.test.ChatState
import com.magicvector.domain.test.RealtimeChatState
import com.magicvector.domain.test.TtsChatState
import com.magicvector.domain.test.WebsocketState
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.utils.test.SSEClient
import com.magicvector.utils.test.TTS_SSEClient
import com.magicvector.utils.test.TestRealtimeChatWsClient
import com.magicvector.utils.test.TestWebSocketClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
import kotlin.math.sqrt

class TestVm : ViewModel() {
    companion object {
        val TAG: String = TestVm::class.java.name
        val GSON = MainApplication.GSON
        const val baseSseUrl = BaseConstant.ConstantUrl.TEST_URL + "/test/stream-sse"
        const val baseSseTTSUrl = BaseConstant.ConstantUrl.TEST_URL + "/test/stream-tts-sse"
        const val baseWebsocketUrl = BaseConstant.ConstantUrl.TEST_WS_URL + "/test-channel"
        const val realtimeChatWsUrl = BaseConstant.ConstantUrl.TEST_WS_URL + "/realtime-no-vad-test"
        const val realtimeChat2WsUrl = BaseConstant.ConstantUrl.TEST_WS_URL + "/realtime-test"
    }

    // ====== 原 TestVm 业务数据（复制实现） ======
    private val customQuestion = "你好啊，你是谁？介绍一下自己吧！"
    val sseChatMessage: MutableLiveData<String> = MutableLiveData("")
    val sseChatState: MutableLiveData<ChatState> = MutableLiveData(ChatState.Idle)
    val ttsSseChatMessage: MutableLiveData<String> = MutableLiveData("")
    val ttsSseChatState: MutableLiveData<TtsChatState> = MutableLiveData(TtsChatState.NotInitialized)
    val websocketAllMessage: MutableLiveData<String> = MutableLiveData("")
    val websocketState: MutableLiveData<WebsocketState> = MutableLiveData(WebsocketState.NotInitialized)
    val audioRecordPlayState: MutableLiveData<AudioRecordPlayState> = MutableLiveData(AudioRecordPlayState.NotInitialized)
    val audioRecordVolume = MutableLiveData(0f)
    val realtimeChatMessage: MutableLiveData<String> = MutableLiveData("")
    val realtimeChatState: MutableLiveData<RealtimeChatState> = MutableLiveData(RealtimeChatState.NotInitialized)
    val realtimeChatVolume = MutableLiveData(0f)
    val realtimeChat2Message: MutableLiveData<String> = MutableLiveData("")
    val realtimeChat2State: MutableLiveData<RealtimeChatState> = MutableLiveData(RealtimeChatState.NotInitialized)
    val realtimeChat2Volume = MutableLiveData(0f)

    private var realtimeChat2WsClient: TestRealtimeChatWsClient? = null
    private var realtimeChatWsClient: TestRealtimeChatWsClient? = null
    private var testWebSocketClient: TestWebSocketClient? = null
    private val realTimeChatSampleRate = 24000
    var realtimeChat2AudioRecord: AudioRecord? = null
    var realtimeChat2AudioTrack: AudioTrack? = null
    var realtimeChatAudioRecord: AudioRecord? = null
    var realtimeChatAudioTrack: AudioTrack? = null
    var recordAudioRecord: AudioRecord? = null
    var recordAudioTrack: AudioTrack? = null
    private var recordAudioBuffer: ByteArrayOutputStream? = null
    private var audioTrack: AudioTrack? = null
    private var isWebsocketEventBusInitialized = false

    private val sseClient = SSEClient(
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build(),
        GSON,
        baseSseUrl
    )
    private val ttsSSEClient = TTS_SSEClient(
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build(),
        GSON,
        baseSseTTSUrl
    )

    // ====== MVI 外层 ======
    private val _uiState = MutableStateFlow(ComposeTestState())
    val uiState: StateFlow<ComposeTestState> = _uiState.asStateFlow()
    private val _effect = Channel<ComposeTestEffect>(Channel.BUFFERED)
    val effect: Flow<ComposeTestEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: ComposeTestIntent) {
        when (intent) {
            ComposeTestIntent.GoEmojiTest -> sendEffect(ComposeTestEffect.RequestCameraForEmojiTest)
            ComposeTestIntent.GoYoloTest -> sendEffect(ComposeTestEffect.RequestCameraForYoloTest)
            ComposeTestIntent.GoVadTest -> sendEffect(ComposeTestEffect.NavigateToVad)
            is ComposeTestIntent.CameraPermissionResultForEmojiTest -> if (intent.granted) sendEffect(ComposeTestEffect.NavigateToEmojiTest) else sendEffect(ComposeTestEffect.ShowToast("请允许相机权限"))
            is ComposeTestIntent.CameraPermissionResultForYoloTest -> if (intent.granted) sendEffect(ComposeTestEffect.NavigateToYoloTest) else sendEffect(ComposeTestEffect.ShowToast("请允许相机权限"))
            is ComposeTestIntent.UpdateRealtime2Question -> _uiState.update { it.copy(realtimeChat2Question = intent.value) }

            ComposeTestIntent.InitRealtimeChat2 -> initRealtimeChat2WsClient()
            ComposeTestIntent.ToggleRecordRealtimeChat2 -> toggleRealtime2Record()
            ComposeTestIntent.StopRealtimeChat2 -> stopRealtimeChat2()
            ComposeTestIntent.SendRealtimeChat2Question -> {
                val q = _uiState.value.realtimeChat2Question
                if (q.isBlank()) sendEffect(ComposeTestEffect.ShowToast("请输入问题")) else {
                    sendRealtime2Question(q)
                    _uiState.update { it.copy(realtimeChat2Question = "") }
                }
            }

            ComposeTestIntent.InitRealtimeChat -> initRealtimeChatWsClient()
            ComposeTestIntent.ToggleRecordRealtimeChat -> toggleRealtimeRecord()
            ComposeTestIntent.StopRealtimeChat -> stopRealtimeChat()

            ComposeTestIntent.InitRecordAudio -> initRecordAudio()
            ComposeTestIntent.BeginRecordAudio -> beginRecordAudio()
            ComposeTestIntent.StopRecordAudio -> stopRecordAudio()
            ComposeTestIntent.PlayRecordAudio -> playRecordAudio()

            ComposeTestIntent.ConnectWebsocket -> connectWebsocket()
            ComposeTestIntent.SendWebsocketMessage -> sendWebsocketMessage("你好啊" + System.currentTimeMillis())
            ComposeTestIntent.DisconnectWebsocket -> disconnectWebsocket()

            ComposeTestIntent.InitializeTtsAudio -> initializeAudioTrack()
            ComposeTestIntent.SendTtsMessage -> sendTTSQuestion()
            ComposeTestIntent.SendSseMessage -> sendQuestion()
        }
    }

    // ====== realtime chat2 ======
    private fun initRealtimeChat2WsClient() {
        realtimeChat2WsClient = TestRealtimeChatWsClient(GSON, realtimeChat2WsUrl)
        initRealtimeChat2RecorderAndPlayer()
        startRealtime2Ws()
        realtimeChat2State.postValue(RealtimeChatState.Initializing)
        syncState()
    }

    @SuppressLint("MissingPermission")
    private fun initRealtimeChat2RecorderAndPlayer() {
        val inChannel = AudioFormat.CHANNEL_IN_MONO
        val outChannel = AudioFormat.CHANNEL_OUT_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        val recBuf = AudioRecord.getMinBufferSize(realTimeChatSampleRate, inChannel, format)
        val playBuf = AudioTrack.getMinBufferSize(realTimeChatSampleRate, outChannel, format)
        realtimeChat2AudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, realTimeChatSampleRate, inChannel, format, recBuf)
        realtimeChat2AudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, realTimeChatSampleRate, outChannel, format, playBuf, AudioTrack.MODE_STREAM)
    }

    private fun startRealtime2Ws() {
        realtimeChat2WsClient?.start(object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { realtimeChat2State.postValue(RealtimeChatState.Disconnected); syncState() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { realtimeChat2State.postValue(RealtimeChatState.Error(t.message ?: "-")); syncState() }
            override fun onMessage(webSocket: WebSocket, text: String) { realtimeChat2State.postValue(RealtimeChatState.Receiving); handleTextMessage2(text); syncState() }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { realtimeChat2State.postValue(RealtimeChatState.Receiving); syncState() }
            override fun onOpen(webSocket: WebSocket, response: Response) {
                realtimeChat2State.postValue(RealtimeChatState.InitializedConnected)
                val request = RealtimeChatConnectRequest().apply {
                    userId = MainApplication.getUserId(); timestamp = System.currentTimeMillis()
                }
                val map = mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.CONNECT.type, RealtimeDataTypeEnum.DATA to GSON.toJson(request))
                realtimeChat2WsClient?.sendMessage(map)
                syncState()
            }
        })
    }

    private fun handleTextMessage2(text: String) {
        val map: Map<String, String> = GSON.fromJson(text, object : TypeToken<Map<String, String>>() {}.type)
        when (RealtimeDataTypeEnum.getByType(map[RealtimeDataTypeEnum.TYPE])) {
            RealtimeDataTypeEnum.START -> { realtimeChat2State.postValue(RealtimeChatState.Receiving); realtimeChat2AudioTrack?.flush(); realtimeChat2AudioTrack?.play() }
            RealtimeDataTypeEnum.STOP -> { realtimeChat2State.postValue(RealtimeChatState.InitializedConnected); realtimeChat2AudioTrack?.stop(); realtimeChat2AudioTrack?.flush() }
            RealtimeDataTypeEnum.AUDIO_CHUNK -> map[RealtimeDataTypeEnum.DATA]?.let { playBase64Audio2(it) }
            RealtimeDataTypeEnum.TEXT_MESSAGE -> {
                val data = map[RealtimeDataTypeEnum.DATA] ?: return
                try {
                    val response: WsChatTextResponse = GSON.fromJson(data, object : TypeToken<WsChatTextResponse>() {}.type)
                    realtimeChat2Message.postValue(response.content ?: "")
                } catch (e: Exception) {
                    Log.e(TAG, "handleTextMessage2 parse error", e)
                }
            }
            else -> {}
        }
    }

    private fun playBase64Audio2(base64Audio: String) {
        val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
        realtimeChat2AudioTrack?.write(audioBytes, 0, audioBytes.size)
    }

    @SuppressLint("MissingPermission")
    private fun startRecordRealtimeChatAudio2() {
        val bufferSize = AudioRecord.getMinBufferSize(realTimeChatSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        realtimeChat2WsClient?.sendMessage(mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.START.type, RealtimeDataTypeEnum.DATA to RealtimeDataTypeEnum.START.name))
        realtimeChat2State.value = RealtimeChatState.RecordingAndSending
        val audioBuffer = ByteArray(bufferSize)
        realtimeChat2AudioRecord?.startRecording()
        Thread {
            while (realtimeChat2State.value == RealtimeChatState.RecordingAndSending) {
                val readSize = realtimeChat2AudioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    val base64Audio = Base64.encodeToString(audioBuffer, 0, readSize, Base64.NO_WRAP)
                    realtimeChat2WsClient?.sendMessage(mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.AUDIO_CHUNK.type, RealtimeDataTypeEnum.DATA to base64Audio))
                    realtimeChat2Volume.postValue(calculateRMSAmplitude(audioBuffer, readSize))
                }
            }
            try { realtimeChat2AudioRecord?.stop() } catch (_: Exception) {}
            realtimeChat2WsClient?.sendMessage(mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.STOP.type, RealtimeDataTypeEnum.DATA to RealtimeDataTypeEnum.STOP.name))
            syncState()
        }.start()
        syncState()
    }

    private fun stopAndSendRealtimeChatAudio2() { realtimeChat2State.postValue(RealtimeChatState.InitializedConnected); syncState() }
    private fun stopRealtimeChat2() { realtimeChat2AudioRecord?.stop(); realtimeChat2AudioRecord?.release(); realtimeChat2AudioTrack?.stop(); realtimeChat2AudioTrack?.release(); realtimeChat2WsClient?.close(); realtimeChat2State.postValue(RealtimeChatState.Disconnected); syncState() }
    private fun sendRealtime2Question(question: String) { realtimeChat2WsClient?.sendMessage(mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.TEXT_MESSAGE.type, RealtimeDataTypeEnum.DATA to question)); syncState() }

    // ====== realtime chat ======
    private fun initRealtimeChatWsClient() {
        realtimeChatWsClient = TestRealtimeChatWsClient(GSON, realtimeChatWsUrl)
        initRealtimeChatRecorderAndPlayer()
        startRealtimeWs()
        realtimeChatState.postValue(RealtimeChatState.Initializing)
        syncState()
    }

    @SuppressLint("MissingPermission")
    private fun initRealtimeChatRecorderAndPlayer() {
        val inChannel = AudioFormat.CHANNEL_IN_MONO
        val outChannel = AudioFormat.CHANNEL_OUT_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        val recBuf = AudioRecord.getMinBufferSize(realTimeChatSampleRate, inChannel, format)
        val playBuf = AudioTrack.getMinBufferSize(realTimeChatSampleRate, outChannel, format)
        realtimeChatAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, realTimeChatSampleRate, inChannel, format, recBuf)
        realtimeChatAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, realTimeChatSampleRate, outChannel, format, playBuf, AudioTrack.MODE_STREAM)
    }

    private fun startRealtimeWs() {
        realtimeChatWsClient?.start(object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { realtimeChatState.postValue(RealtimeChatState.Disconnected); syncState() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { realtimeChatState.postValue(RealtimeChatState.Error(t.message ?: "-")); syncState() }
            override fun onMessage(webSocket: WebSocket, text: String) { realtimeChatState.postValue(RealtimeChatState.Receiving); handleTextMessage(text); syncState() }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { realtimeChatState.postValue(RealtimeChatState.Receiving); syncState() }
            override fun onOpen(webSocket: WebSocket, response: Response) { realtimeChatState.postValue(RealtimeChatState.InitializedConnected); syncState() }
        })
    }

    private fun handleTextMessage(text: String) {
        val map: Map<String, String> = GSON.fromJson(text, object : TypeToken<Map<String, String>>() {}.type)
        when (RealtimeDataTypeEnum.getByType(map[RealtimeDataTypeEnum.TYPE])) {
            RealtimeDataTypeEnum.START -> { realtimeChatState.postValue(RealtimeChatState.Receiving); recordAudioTrack?.flush(); realtimeChatAudioTrack?.play() }
            RealtimeDataTypeEnum.STOP -> { realtimeChatState.postValue(RealtimeChatState.InitializedConnected); realtimeChatAudioTrack?.stop(); recordAudioTrack?.flush() }
            RealtimeDataTypeEnum.AUDIO_CHUNK -> map[RealtimeDataTypeEnum.DATA]?.let { playBase64Audio(it) }
            RealtimeDataTypeEnum.TEXT_MESSAGE -> map[RealtimeDataTypeEnum.DATA]?.let { realtimeChatMessage.postValue(it) }
            else -> {}
        }
    }

    private fun playBase64Audio(base64Audio: String) { val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT); realtimeChatAudioTrack?.write(audioBytes, 0, audioBytes.size) }

    @SuppressLint("MissingPermission")
    private fun startRecordRealtimeChatAudio() {
        val bufferSize = AudioRecord.getMinBufferSize(realTimeChatSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        realtimeChatWsClient?.sendMessage(mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.START.type, RealtimeDataTypeEnum.DATA to RealtimeDataTypeEnum.START.name))
        realtimeChatState.value = RealtimeChatState.RecordingAndSending
        val audioBuffer = ByteArray(bufferSize)
        realtimeChatAudioRecord?.startRecording()
        Thread {
            while (realtimeChatState.value == RealtimeChatState.RecordingAndSending) {
                val readSize = realtimeChatAudioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    val base64Audio = Base64.encodeToString(audioBuffer, 0, readSize, Base64.NO_WRAP)
                    realtimeChatWsClient?.sendMessage(mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.AUDIO_CHUNK.type, RealtimeDataTypeEnum.DATA to base64Audio))
                    realtimeChatVolume.postValue(calculateRMSAmplitude(audioBuffer, readSize))
                }
            }
            try { realtimeChatAudioRecord?.stop() } catch (_: Exception) {}
            realtimeChatWsClient?.sendMessage(mapOf(RealtimeDataTypeEnum.TYPE to RealtimeDataTypeEnum.STOP.type, RealtimeDataTypeEnum.DATA to RealtimeDataTypeEnum.STOP.name))
            syncState()
        }.start()
        syncState()
    }

    private fun stopAndSendRealtimeChatAudio() { realtimeChatState.postValue(RealtimeChatState.InitializedConnected); syncState() }
    private fun stopRealtimeChat() { realtimeChatAudioRecord?.stop(); realtimeChatAudioRecord?.release(); realtimeChatAudioTrack?.stop(); realtimeChatAudioTrack?.release(); realtimeChatWsClient?.close(); realtimeChatState.postValue(RealtimeChatState.Disconnected); syncState() }

    // ====== audio record/play ======
    @SuppressLint("MissingPermission")
    private fun initRecordAudio() {
        audioRecordPlayState.value = AudioRecordPlayState.Initializing
        val sampleRate = 24000
        val inChannel = AudioFormat.CHANNEL_IN_MONO
        val outChannel = AudioFormat.CHANNEL_OUT_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        val recBuf = AudioRecord.getMinBufferSize(sampleRate, inChannel, format)
        val playBuf = AudioTrack.getMinBufferSize(sampleRate, outChannel, format)
        recordAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, inChannel, format, recBuf)
        recordAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, outChannel, format, playBuf, AudioTrack.MODE_STREAM)
        recordAudioBuffer = ByteArrayOutputStream()
        audioRecordPlayState.value = AudioRecordPlayState.Ready
        syncState()
    }

    @SuppressLint("MissingPermission")
    private fun beginRecordAudio() {
        recordAudioRecord?.startRecording()
        audioRecordPlayState.value = AudioRecordPlayState.Recording
        Thread {
            val buffer = ByteArray(1024)
            while (audioRecordPlayState.value == AudioRecordPlayState.Recording) {
                val read = recordAudioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    recordAudioBuffer?.write(buffer, 0, read)
                    audioRecordVolume.postValue(calculateRMSAmplitude(buffer, read))
                }
            }
            syncState()
        }.start()
        syncState()
    }

    @SuppressLint("DefaultLocale")
    private fun stopRecordAudio() {
        recordAudioRecord?.stop()
        recordAudioRecord?.release()
        val audioSize = recordAudioBuffer?.size() ?: 0
        val duration = audioSize / (24000 * 1 * 2.0)
        val msg = "录音文件信息：\n文件大小：${String.format("%.1f", audioSize / 1024.0)}KB\n时长：${String.format("%.1f", duration)}秒\n"
        audioRecordPlayState.value = AudioRecordPlayState.RecordedAndPlayable(msg)
        syncState()
    }

    private fun playRecordAudio() {
        if (recordAudioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            audioRecordPlayState.value = AudioRecordPlayState.Error("播放器未正确初始化")
            syncState()
            return
        }
        recordAudioTrack?.stop()
        recordAudioTrack?.flush()
        recordAudioTrack?.play()
        audioRecordPlayState.value = AudioRecordPlayState.Playing
        recordAudioBuffer?.toByteArray()?.let { audioTrack?.write(it, 0, it.size) }
        audioRecordPlayState.value = AudioRecordPlayState.PlayedEnd
        syncState()
    }

    fun calculateRMSAmplitude(buffer: ByteArray, bytesRead: Int): Float {
        if (bytesRead < 2) return 0f
        var sumSquares = 0.0
        var sampleCount = 0
        for (i in 0 until bytesRead - 1 step 2) {
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

    // ====== websocket ======
    private fun connectWebsocket() {
        websocketState.value = WebsocketState.Initializing
        initWebsocketEventBus()
        testWebSocketClient = TestWebSocketClient(GSON, baseWebsocketUrl)
        websocketState.value = WebsocketState.InitializedNotConnected
        testWebSocketClient?.start()
        syncState()
    }
    private fun sendWebsocketMessage(message: String) { websocketState.value = WebsocketState.Sending; testWebSocketClient?.sendMessage(message); syncState() }
    private fun disconnectWebsocket() { testWebSocketClient?.close(); websocketState.value = WebsocketState.Disconnected; syncState() }
    private fun initWebsocketEventBus() { if (!isWebsocketEventBusInitialized) { EventBus.getDefault().register(this); isWebsocketEventBusInitialized = true } }
    private fun unregisterWebsocketEventBus() { if (isWebsocketEventBusInitialized) { EventBus.getDefault().unregister(this); isWebsocketEventBusInitialized = false } }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWebSocketMessageEvent(event: WebSocketMessageEvent) {
        when (event.eventType) {
            WebsocketEventTypeEnum.ON_OPEN -> websocketState.postValue(WebsocketState.Connected)
            WebsocketEventTypeEnum.ON_MESSAGE, WebsocketEventTypeEnum.ON_MESSAGE_BYTE -> websocketState.postValue(WebsocketState.Receiving)
            WebsocketEventTypeEnum.ON_CLOSING, WebsocketEventTypeEnum.ON_CLOSED -> websocketState.postValue(WebsocketState.Disconnected)
            WebsocketEventTypeEnum.ON_FAILURE -> websocketState.postValue(WebsocketState.Error(event.text))
            WebsocketEventTypeEnum.SEND_MESSAGE -> websocketState.postValue(WebsocketState.Sending)
        }
        websocketAllMessage.postValue((websocketAllMessage.value ?: "") + "\n[${event.eventType.desc}]: ${event.text}")
        syncState()
    }

    // ====== sse/tts ======
    private fun sendQuestion() {
        viewModelScope.launch {
            sseChatState.value = ChatState.Loading
            sseChatMessage.value = ""
            sseClient.streamChat(customQuestion).collect { data ->
                sseChatMessage.value += data
                sseChatState.value = ChatState.Streaming
                syncState()
            }
            sseChatState.value = ChatState.Success
            syncState()
        }
    }

    private fun sendTTSQuestion() {
        viewModelScope.launch {
            ttsSseChatState.value = TtsChatState.Loading
            ttsSseChatMessage.value = ""
            ttsSSEClient.streamTTSChat(customQuestion).collect { dataMap ->
                when (dataMap["type"] ?: "error") {
                    "text" -> {
                        val text = dataMap["data"] ?: ""
                        ttsSseChatMessage.value += text
                        ttsSseChatState.value = TtsChatState.Streaming
                    }
                    "audio" -> {
                        val base64Audio = dataMap["data"] ?: ""
                        ttsPlayAudio(base64Audio)
                    }
                }
                syncState()
            }
            ttsSseChatState.value = TtsChatState.Success
            syncState()
        }
    }

    private fun initializeAudioTrack() {
        if (ttsSseChatState.value !is TtsChatState.NotInitialized && ttsSseChatState.value !is TtsChatState.InitializationFailed) return
        try {
            ttsSseChatState.value = TtsChatState.Initializing
            val sampleRate = 24000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val format = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, format)
            audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, format, minBufferSize, AudioTrack.MODE_STREAM)
            audioTrack?.play()
            ttsSseChatState.value = TtsChatState.Idle
        } catch (e: Exception) {
            ttsSseChatState.value = TtsChatState.InitializationFailed(e.message ?: "未知错误")
        }
        syncState()
    }

    private fun ttsPlayAudio(base64Data: String) {
        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (audioTrack == null) initializeAudioTrack()
            audioTrack?.write(audioBytes, 0, audioBytes.size)
        } catch (_: Exception) {
        }
    }

    private fun releaseAudioResources() {
        audioTrack?.let {
            it.stop()
            it.release()
            audioTrack = null
        }
        ttsSseChatState.value = TtsChatState.NotInitialized
    }

    // ====== ui sync ======
    private fun toggleRealtime2Record() {
        when (realtimeChat2State.value) {
            is RealtimeChatState.InitializedConnected -> startRecordRealtimeChatAudio2()
            is RealtimeChatState.RecordingAndSending -> stopAndSendRealtimeChatAudio2()
            else -> sendEffect(ComposeTestEffect.ShowToast("当前状态异常"))
        }
    }
    private fun toggleRealtimeRecord() {
        when (realtimeChatState.value) {
            is RealtimeChatState.InitializedConnected -> startRecordRealtimeChatAudio()
            is RealtimeChatState.RecordingAndSending -> stopAndSendRealtimeChatAudio()
            else -> sendEffect(ComposeTestEffect.ShowToast("当前状态异常"))
        }
    }

    private fun syncState() {
        _uiState.update {
            it.copy(
                realtimeChat2State = realtimeChat2State.value ?: RealtimeChatState.NotInitialized,
                realtimeChat2Message = realtimeChat2Message.value ?: "",
                realtimeChat2Volume = realtimeChat2Volume.value ?: 0f,
                realtimeChatState = realtimeChatState.value ?: RealtimeChatState.NotInitialized,
                realtimeChatMessage = realtimeChatMessage.value ?: "",
                realtimeChatVolume = realtimeChatVolume.value ?: 0f,
                audioRecordPlayState = audioRecordPlayState.value ?: AudioRecordPlayState.NotInitialized,
                audioRecordVolume = audioRecordVolume.value ?: 0f,
                websocketState = websocketState.value ?: WebsocketState.NotInitialized,
                websocketMessageHistory = websocketAllMessage.value ?: "",
                ttsSseState = ttsSseChatState.value ?: TtsChatState.NotInitialized,
                ttsSseMessage = ttsSseChatMessage.value ?: "",
                sseState = sseChatState.value ?: ChatState.Idle,
                sseMessage = sseChatMessage.value ?: "",
            )
        }
    }

    private fun sendEffect(effect: ComposeTestEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    override fun onCleared() {
        super.onCleared()
        releaseAudioResources()
        unregisterWebsocketEventBus()
    }
}

@Stable
data class ComposeTestState(
    val realtimeChat2Question: String = "",
    val realtimeChat2State: RealtimeChatState = RealtimeChatState.NotInitialized,
    val realtimeChat2Message: String = "",
    val realtimeChat2Volume: Float = 0f,
    val realtimeChatState: RealtimeChatState = RealtimeChatState.NotInitialized,
    val realtimeChatMessage: String = "",
    val realtimeChatVolume: Float = 0f,
    val audioRecordPlayState: AudioRecordPlayState = AudioRecordPlayState.NotInitialized,
    val audioRecordVolume: Float = 0f,
    val websocketState: WebsocketState = WebsocketState.NotInitialized,
    val websocketMessageHistory: String = "",
    val ttsSseState: TtsChatState = TtsChatState.NotInitialized,
    val ttsSseMessage: String = "",
    val sseState: ChatState = ChatState.Idle,
    val sseMessage: String = "",
)

sealed class ComposeTestIntent {
    data object GoEmojiTest : ComposeTestIntent()
    data object GoYoloTest : ComposeTestIntent()
    data object GoVadTest : ComposeTestIntent()
    data class CameraPermissionResultForEmojiTest(val granted: Boolean) : ComposeTestIntent()
    data class CameraPermissionResultForYoloTest(val granted: Boolean) : ComposeTestIntent()
    data class UpdateRealtime2Question(val value: String) : ComposeTestIntent()

    data object InitRealtimeChat2 : ComposeTestIntent()
    data object ToggleRecordRealtimeChat2 : ComposeTestIntent()
    data object StopRealtimeChat2 : ComposeTestIntent()
    data object SendRealtimeChat2Question : ComposeTestIntent()

    data object InitRealtimeChat : ComposeTestIntent()
    data object ToggleRecordRealtimeChat : ComposeTestIntent()
    data object StopRealtimeChat : ComposeTestIntent()

    data object InitRecordAudio : ComposeTestIntent()
    data object BeginRecordAudio : ComposeTestIntent()
    data object StopRecordAudio : ComposeTestIntent()
    data object PlayRecordAudio : ComposeTestIntent()

    data object ConnectWebsocket : ComposeTestIntent()
    data object SendWebsocketMessage : ComposeTestIntent()
    data object DisconnectWebsocket : ComposeTestIntent()

    data object InitializeTtsAudio : ComposeTestIntent()
    data object SendTtsMessage : ComposeTestIntent()
    data object SendSseMessage : ComposeTestIntent()
}

sealed class ComposeTestEffect {
    data object RequestCameraForEmojiTest : ComposeTestEffect()
    data object RequestCameraForYoloTest : ComposeTestEffect()
    data object NavigateToEmojiTest : ComposeTestEffect()
    data object NavigateToYoloTest : ComposeTestEffect()
    data object NavigateToVad : ComposeTestEffect()
    data class ShowToast(val message: String) : ComposeTestEffect()
}
