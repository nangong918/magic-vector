package com.magicvector.manager

import android.Manifest
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.magicvector.repository.api.config.ApiUrlConfig
import com.magicvector.domain.model.message.MessageContactItemModel
import com.data.domain.ao.mixLLM.McpSwitch
import com.data.domain.ao.mixLLM.MixLLMEvent
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.VadChatState
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.constant.chat.RealtimeResponseDataTypeEnum
import com.data.domain.constant.chat.RealtimeSystemResponseEventEnum
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.manager.audio.AudioController
import com.magicvector.manager.audio.AudioHandleCallback
import com.magicvector.manager.audio.IsAudioRecording
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.audio.vad.VadDetectionCallback
import com.magicvector.manager.vl.UdpVisionManager
import com.magicvector.manager.ws.WsManager
import com.magicvector.utils.chat.RealtimeChatWsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import com.data.domain.constant.chat.MessageTypeEnum
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.domain.vo.message.ChatMessageVO
import com.magicvector.manager.event.chat.ChatEventManager
import com.magicvector.utils.sort.PageDirection
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 实时聊天控制器 - MVI 设计模式
 * 状态驱动，事件流管理
 * 维护：
 * 1. WS消息长连接
 * 2. VAD语音活动检测（持续录音）
 * 3. YOLOv8物体检测（通过UDP持续发送视频帧）
 * 4. ChatEventManager历史消息
 */
class RealtimeChatController : IsAudioRecording {

    companion object {
        const val TAG = "RealtimeChatController"
        val GSON = MainApplication.GSON
    }

    // ========== MVI State ==========
    private val _uiState = MutableStateFlow(RealtimeChatUiState())
    val uiState: StateFlow<RealtimeChatUiState> = _uiState.asStateFlow()

    private val _realtimeState = MutableStateFlow<RealtimeChatState>(RealtimeChatState.NotInitialized)
    val realtimeState: StateFlow<RealtimeChatState> = _realtimeState.asStateFlow()

    // ========== Events (替代回调) ==========
    private val _vadStateEvents = MutableSharedFlow<VadChatState>()
    val vadStateEvents: SharedFlow<VadChatState> = _vadStateEvents.asSharedFlow()

    private val _agentTextEvents = MutableSharedFlow<String>()
    val agentTextEvents: SharedFlow<String> = _agentTextEvents.asSharedFlow()

    // ========== Data ==========
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var udpVisionManager: UdpVisionManager? = null
    var messageContactItemModel: MessageContactItemModel? = null

    // ========== Network ==========
    private var realtimeChatWsClient: RealtimeChatWsClient? = null
    private var userId: Long? = null
    private var agentId: Long? = null
    private val manualWsClosing = AtomicBoolean(false)

    // ========== Audio ==========
    var audioController: AudioController? = null

    // ========== Handlers ==========
    private var handleSystemResponse: HandleSystemResponse? = null
    private var chatEventManager: ChatEventManager? = null

    // ========== Vision 视频帧回调 ==========
    private var onVideoFrameCallback: ((Bitmap) -> Unit)? = null

    // ========== MVI Intent ==========
    sealed class Intent {
        data class Initialize(
            val chatActivity: FragmentActivity,
            val ao: MessageContactItemModel?,
            val chatAAo: ChatAAo,
            val initNetworkRunnable: () -> Job,
            val onVideoFrame: ((Bitmap) -> Unit)? = null
        ) : Intent()
        data class BindChannel(val agentId: Long) : Intent()
        data class SendTextMessage(val message: String) : Intent()
        data object StartVadCall : Intent()
        data object StopVadCall : Intent()
        data object DestroyVadCall : Intent()
        data class SendMcpSwitch(val mcpSwitch: McpSwitch) : Intent()
        data object RefreshMessages : Intent()
        data object LoadMoreMessages : Intent()
        data object ClearCache : Intent()
        data class SendVideoFrame(val bitmap: Bitmap) : Intent()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun processIntent(intent: Intent) {
        when (intent) {
            is Intent.Initialize -> initialize(intent)
            is Intent.BindChannel -> bindChannel(intent.agentId)
            is Intent.SendTextMessage -> sendTextMessage(intent.message)
            Intent.StartVadCall -> startVadCall()
            Intent.StopVadCall -> stopVadCall()
            Intent.DestroyVadCall -> destroyVadCall()
            is Intent.SendMcpSwitch -> sendMcpSwitch(intent.mcpSwitch)
            Intent.RefreshMessages -> refreshMessages()
            Intent.LoadMoreMessages -> loadMoreMessages()
            Intent.ClearCache -> clearCache()
            is Intent.SendVideoFrame -> sendVideoFrame(intent.bitmap)
        }
    }

    // ========== Intent 实现 ==========
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initialize(intent: Intent.Initialize) {
        _uiState.update { it.copy(isLoading = true) }

        this.messageContactItemModel = intent.ao
        this.onVideoFrameCallback = intent.onVideoFrame

        intent.ao?.contactId?.toLongOrNull()?.let { agentId ->
            this.agentId = agentId
            chatEventManager = MainApplication.getChatEventMapManager().getOrCreateManager(agentId)
        }

        intent.chatAAo.nameLd.postValue(intent.ao?.vo?.name ?: "")
        intent.chatAAo.avatarUrlLd.postValue(intent.ao?.vo?.avatarUrl ?: "")

        if (messageContactItemModel?.contactId != null) {
            _realtimeState.value = RealtimeChatState.Initializing
            ensureUserConnection(MainApplication.getUserId())
            bindChannel(messageContactItemModel!!.contactId!!.toLong())
        } else {
            _realtimeState.value = RealtimeChatState.Error("Agent Id is Null")
            throw IllegalArgumentException("Agent Id is Null")
        }

        // 初始化UDP视觉管理器
        initUdpVisionManager()

        intent.initNetworkRunnable.invoke()
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun initUdpVisionManager() {
        val userIdStr = MainApplication.getUserId()
        val agentIdStr = agentId?.toString() ?: return
        udpVisionManager = UdpVisionManager.getInstance().apply {
            initialize(userIdStr, agentIdStr)
        }
    }

    private fun sendVideoFrame(bitmap: Bitmap) {
        // 回调给外部（用于YOLOv8识别）
        onVideoFrameCallback?.invoke(bitmap)

        // 通过UDP发送视频帧给后端
        udpVisionManager?.sendVideoFrame(bitmap)
    }

    private fun bindChannel(agentId: Long) {
        this.agentId = agentId
        realtimeChatWsClient?.let { client ->
            if (_realtimeState.value == RealtimeChatState.InitializedConnected ||
                _realtimeState.value == RealtimeChatState.Receiving ||
                _realtimeState.value == RealtimeChatState.RecordingAndSending
            ) {
                WsManager.sendBindChannelInfo(agentId.toString(), client)
            }
        }
    }

    private fun refreshMessages() {
        cacheScope.launch {
            chatEventManager?.onHttpFullLoad()
        }
    }

    private fun loadMoreMessages() {
        cacheScope.launch {
            try {
                chatEventManager?.onHttpPageLoad(PageDirection.UP, 20)
            } catch (e: Exception) {
                Log.e(TAG, "loadMoreMessages failed", e)
            }
        }
    }

    private fun clearCache() {
        chatEventManager?.clearCache()
    }

    // ========== WebSocket 管理 ==========
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun ensureUserConnection(userId: String) {
        if (userId.isBlank()) {
            Log.w(TAG, "ensureUserConnection: userId is blank")
            return
        }
        this.userId = userId.toLongOrNull()
        realtimeChatWsClient = initRealtimeChatWsClient()
        initAudioController()
        audioController?.initAudioRecorderAndPlayer()

        if (!MainApplication.getNetworkManager().refreshNetworkState().isNetworkOnline) {
            _realtimeState.value = RealtimeChatState.Disconnected
            Log.i(TAG, "ensureUserConnection: network offline, skip ws connect")
            return
        }
        startRealtimeWs()
    }

    private fun initRealtimeChatWsClient(): RealtimeChatWsClient {
        return realtimeChatWsClient ?: synchronized(this) {
            realtimeChatWsClient ?: RealtimeChatWsClient(
                GSON,
                ApiUrlConfig.getWsMainUrl() + BaseConstant.WSConstantUrl.AGENT_REALTIME_CHAT_URL
            ).also { realtimeChatWsClient = it }
        }
    }

    private fun startRealtimeWs(forceReconnect: Boolean = false) {
        if (!forceReconnect && (_realtimeState.value == RealtimeChatState.InitializedConnected ||
                    _realtimeState.value == RealtimeChatState.Receiving ||
                    _realtimeState.value == RealtimeChatState.RecordingAndSending
                    )) {
            realtimeChatWsClient?.let { client ->
                userId?.let { WsManager.sendConnectInfo(it.toString(), client) }
                agentId?.let { WsManager.sendBindChannelInfo(it.toString(), client) }
            }
            return
        }

        realtimeChatWsClient?.let { client ->
            manualWsClosing.set(false)
            val listener = createWebSocketListener(client)
            if (forceReconnect) {
                client.reconnect(listener)
            } else {
                client.start(listener)
            }
        }
    }

    private fun createWebSocketListener(client: RealtimeChatWsClient): WebSocketListener {
        return object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _realtimeState.value = RealtimeChatState.Disconnected
                val shouldReconnect = !manualWsClosing.getAndSet(false)
                MainApplication.getNetworkManager().onWebSocketDisconnected(shouldReconnect)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "realtimeChatWsClient::onFailure: ${t.message}")
                _realtimeState.value = RealtimeChatState.Error(t.message ?: "-")
                val shouldReconnect = !manualWsClosing.getAndSet(false)
                MainApplication.getNetworkManager().onWebSocketDisconnected(shouldReconnect)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _realtimeState.value = RealtimeChatState.Receiving
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                _realtimeState.value = RealtimeChatState.Receiving
                Log.i(TAG, "收到字节信息::长度: ${bytes.size}")
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                _realtimeState.value = RealtimeChatState.InitializedConnected
                MainApplication.getNetworkManager().onWebSocketConnected()
                userId?.let { WsManager.sendConnectInfo(it.toString(), client) }
                agentId?.let { WsManager.sendBindChannelInfo(it.toString(), client) }
            }
        }
    }

    // ========== 消息处理 ==========
    private fun handleTextMessage(text: String) {
        val chatWsTextMessageParseResult = WsManager.getTextMessageDataType(text) ?: return
        val type = chatWsTextMessageParseResult.responseType
        val map = chatWsTextMessageParseResult.map

        when (type) {
            RealtimeResponseDataTypeEnum.START_TTS -> handleStartTts()
            RealtimeResponseDataTypeEnum.STOP_TTS -> handleStopTts()
            RealtimeResponseDataTypeEnum.AUDIO_CHUNK -> handleAudioChunk(map)
            RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE -> handleTextChatResponse(map)
            RealtimeResponseDataTypeEnum.TEXT_SYSTEM_RESPONSE -> handleSystemMessage(map)
            RealtimeResponseDataTypeEnum.EVENT_LIST -> handleEventList(map)
            else -> {}
        }
    }

    private fun handleStartTts() {
        _realtimeState.value = RealtimeChatState.Receiving
        cacheScope.launch { _vadStateEvents.emit(VadChatState.Replying) }
        audioController?.startAudioTrackPlay()
    }

    private fun handleStopTts() {
        _realtimeState.value = RealtimeChatState.InitializedConnected
        cacheScope.launch { _vadStateEvents.emit(VadChatState.Silent) }
        audioController?.stopAudioTrackPlay()
    }

    private fun handleAudioChunk(map: Map<String, String>) {
        _realtimeState.value = RealtimeChatState.Receiving
        val data = map[RealtimeResponseDataTypeEnum.DATA]
        data?.let {
            audioController?.playBase64Audio(base64Audio = it, isShowLog = true)
        }
    }

    private fun handleTextChatResponse(map: Map<String, String>) {
        _realtimeState.value = RealtimeChatState.Receiving
        val data = map[RealtimeResponseDataTypeEnum.DATA]
        data?.let {
            handleTextMessageResponse(it)
        }
    }

    private fun handleTextMessageResponse(message: String) {
        val response = runCatching {
            GSON.fromJson(message, com.magicvector.domain.dto.ws.response.WsChatTextResponse::class.java)
        }.getOrNull() ?: return

        // 更新到 ChatEventManager
        val chatMessage = ChatMessageModel(
            chatMessageVo = ChatMessageVO(
                briefMessageVo = ChatBriefMessageVO(
                    content = response.content,
                    chatTime = response.chatTime,
                    role = response.role
                ),
                imgUrl = "",
                messageType = MessageTypeEnum.TEXT.value
            ),
            agentId = response.agentId.toLongOrNull() ?: agentId ?: 0L,
            userId = response.userId.toLongOrNull() ?: userId ?: 0L,
            messageId = response.messageId.toLongOrNull() ?: 0L,
            timestamp = response.timestamp
        )

        cacheScope.launch {
            chatEventManager?.onWsUpsertOne(chatMessage)
            _agentTextEvents.emit(response.content)
        }
    }

    private fun handleSystemMessage(map: Map<String, String>) {
        val data = map[RealtimeResponseDataTypeEnum.DATA] ?: return
        try {
            val systemMap: Map<String, String> = GSON.fromJson(data, object : TypeToken<Map<String, String>>() {}.type)
            if (systemMap[RealtimeSystemResponseEventEnum.EVENT_KET] != null) {
                handleSystemResponse?.handleSystemResponse(systemMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleSystemMessage: parse error", e)
        }
    }

    private fun handleEventList(map: Map<String, String>) {
        val data = map[RealtimeResponseDataTypeEnum.DATA] ?: return
        try {
            val eventList: List<MixLLMEvent> = GSON.fromJson(data, object : TypeToken<List<MixLLMEvent>>() {}.type)
            eventList.forEach { event ->
                Log.d(TAG, "Event Type: ${event.eventType}, Event Data: ${event.event}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleEventList: parse error", e)
        }
    }

    // ========== 发送消息 ==========
    private fun sendTextMessage(message: String) {
        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to message
        )
        realtimeChatWsClient?.sendMessage(dataMap)
    }

    fun sendMcpSwitch(mcpSwitch: McpSwitch = MainApplication.getMcpSwitch()) {
        val mcpSwitchJson = GSON.toJson(mcpSwitch)
        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to mcpSwitchJson
        )
        realtimeChatWsClient?.sendMessage(dataMap)
    }

    // ========== 音频处理（持续录音） ==========
    private fun initAudioController() {
        if (audioController == null) {
            audioController = AudioController(
                audioHandleCallback = createAudioHandleCallback(),
                vadDetectionCallback = createVadDetectionCallback()
            )
        }
    }

    private fun createAudioHandleCallback(): AudioHandleCallback {
        return object : AudioHandleCallback {
            override fun onPlayBase64Audio(base64Audio: String) {
                cacheScope.launch { _vadStateEvents.emit(VadChatState.Replying) }
            }

            override fun onStartRecording() {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.name
                )
                realtimeChatWsClient?.sendMessage(dataMap)
                _realtimeState.value = RealtimeChatState.RecordingAndSending
            }

            override fun onObtainAudio(base64Audio: String) {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                    RealtimeRequestDataTypeEnum.DATA to base64Audio
                )
                realtimeChatWsClient?.sendMessage(dataMap)
            }

            override fun onStopRecording() {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                realtimeChatWsClient?.sendMessage(dataMap)
            }
        }
    }

    private fun createVadDetectionCallback(): VadDetectionCallback {
        return object : VadDetectionCallback {
            override fun onStartSpeech(audioBuffer: ByteArray) {
                sendAudioData(audioBuffer, isStart = true)
                cacheScope.launch { _vadStateEvents.emit(VadChatState.Speaking) }
            }

            override fun speeching(audioBuffer: ByteArray) {
                sendAudioData(audioBuffer)
                cacheScope.launch { _vadStateEvents.emit(VadChatState.Speaking) }
            }

            override fun onStopSpeech() {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                realtimeChatWsClient?.sendMessage(dataMap)
                cacheScope.launch { _vadStateEvents.emit(VadChatState.Silent) }
            }
        }
    }

    private fun sendAudioData(audioBuffer: ByteArray, isStart: Boolean = false) {
        if (isStart) {
            val startMap = mapOf(
                RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.type,
                RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.name
            )
            realtimeChatWsClient?.sendMessage(startMap)
        }

        if (audioBuffer.isNotEmpty()) {
            val base64Audio = Base64.encodeToString(audioBuffer, 0, audioBuffer.size, Base64.NO_WRAP)
            val dataMap = mapOf(
                RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                RealtimeRequestDataTypeEnum.DATA to base64Audio
            )
            realtimeChatWsClient?.sendMessage(dataMap)
        }
    }

    private fun startVadCall() {
        audioController?.startVAD(onStart = {
            cacheScope.launch { _vadStateEvents.emit(VadChatState.Silent) }
        })
    }

    private fun stopVadCall() {
        audioController?.stopVAD(onStop = {
            cacheScope.launch { _vadStateEvents.emit(VadChatState.Muted) }
        })
    }

    private fun destroyVadCall() {
        audioController?.releaseVADController()
        cacheScope.launch { _vadStateEvents.emit(VadChatState.Muted) }
    }

    override fun isAudioRecording(): Boolean {
        return _realtimeState.value == RealtimeChatState.RecordingAndSending
    }

    fun setHandleSystemResponse(handleSystemResponse: HandleSystemResponse?) {
        this.handleSystemResponse = handleSystemResponse
    }

    fun reconnectUserConnectionIfNeeded() {
        val userId = userId ?: return
        if (!MainApplication.getNetworkManager().refreshNetworkState().isNetworkOnline) return
        realtimeChatWsClient = initRealtimeChatWsClient()
        startRealtimeWs(forceReconnect = true)
    }

    // ========== 生命周期 ==========
    fun releaseAllResource() {
        messageContactItemModel = null
        _realtimeState.value = RealtimeChatState.NotInitialized

        realtimeChatWsClient?.let {
            manualWsClosing.set(true)
            it.close()
            realtimeChatWsClient = null
            MainApplication.getNetworkManager().onWebSocketDisconnected(shouldReconnect = false)
        }

        audioController?.releaseAll()
        audioController = null

        udpVisionManager?.destroy()
        udpVisionManager = null
    }

    fun destroy() {
        releaseAllResource()
        cacheScope.cancel()
    }
}

// ========== State 定义 ==========
data class RealtimeChatUiState(
    val isLoading: Boolean = false,
    val agentText: String = ""
)

// ========== 扩展 State ==========
open class RealtimeChatState {
    // 未初始化
    object NotInitialized : RealtimeChatState()
    // 正在初始化
    object Initializing : RealtimeChatState()
    // 已初始化并且连接
    object InitializedConnected : RealtimeChatState()
    // 正在记录消息
    object RecordingAndSending : RealtimeChatState()
    // 正在接收消息
    object Receiving : RealtimeChatState()
    // 断开连接
    object Disconnected : RealtimeChatState()
    // 错误
    data class Error(val message: String) : RealtimeChatState()
}