package com.magicvector.manager

import android.Manifest
import android.os.Handler
import android.os.Looper
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
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.callback.OnReceiveAgentTextCallback
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import com.data.domain.constant.chat.MessageTypeEnum
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.domain.vo.message.ChatMessageVO
import com.magicvector.manager.event.chat.ChatEventManager
import com.magicvector.utils.sort.PageDirection

/**
 * 实时聊天控制器 - MVI 设计模式
 * 状态驱动，事件流管理
 * 维护：
 * 1. WS消息长连接
 * 2. VAD语音活动检测
 * 3. YOLOv8物体检测
 * 4. ChatEventManager历史消息
 */
class RealtimeChatController : IsAudioRecording {

    companion object {
        const val TAG = "RealtimeChatController"
        val GSON = MainApplication.GSON
        val mainHandler: Handler = Handler(Looper.getMainLooper())
    }

    // ========== MVI State ==========
    private val _uiState = MutableStateFlow(RealtimeChatUiState())
    val uiState: StateFlow<RealtimeChatUiState> = _uiState.asStateFlow()

    private val _realtimeState = MutableStateFlow<RealtimeChatState>(RealtimeChatState.NotInitialized)
    val realtimeState: StateFlow<RealtimeChatState> = _realtimeState.asStateFlow()

    // ========== Data ==========
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val currentIsEmoji = AtomicBoolean(false)
    private var onVadChatStateChange: OnVadChatStateChange? = null
    private var onReceiveAgentTextCallback: OnReceiveAgentTextCallback? = null
    var messageContactItemModel: MessageContactItemModel? = null
    var currentAgentId: Long = 0L

    // ========== Network ==========
    private var realtimeChatWsClient: RealtimeChatWsClient? = null
    private var userId: Long? = null
    private var agentId: Long? = null

    // ========== Audio ==========
    var audioController: AudioController? = null
    var isChatCalling: AtomicBoolean? = null

    // ========== Handlers ==========
    private var handleSystemResponse: HandleSystemResponse? = null
    private var chatEventManager: ChatEventManager? = null

    // ========== MVI Intent ==========
    sealed class Intent {
        data class Initialize(
            val chatActivity: FragmentActivity,
            val ao: MessageContactItemModel?,
            val chatAAo: ChatAAo,
            val initNetworkRunnable: () -> Job,
            val onReceiveAgentTextCallback: OnReceiveAgentTextCallback,
            val onVadChatStateChange: OnVadChatStateChange
        ) : Intent()
        data class BindChannel(val agentId: Long) : Intent()
        data class SendTextMessage(val message: String) : Intent()
        data class StartRecordAudio(val scope: CoroutineScope) : Intent()
        data object StopRecordAudio : Intent()
        data object StartVadCall : Intent()
        data object StopVadCall : Intent()
        data object DestroyVadCall : Intent()
        data class SetEmojiMode(val isEmoji: Boolean) : Intent()
        data class SendMcpSwitch(val mcpSwitch: McpSwitch) : Intent()
        data object RefreshMessages : Intent()
        data object LoadMoreMessages : Intent()
        data object ClearCache : Intent()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun processIntent(intent: Intent) {
        when (intent) {
            is Intent.Initialize -> initialize(intent)
            is Intent.BindChannel -> bindChannel(intent.agentId)
            is Intent.SendTextMessage -> sendTextMessage(intent.message)
            is Intent.StartRecordAudio -> startRecordAudio(intent.scope)
            Intent.StopRecordAudio -> stopRecordAudio()
            Intent.StartVadCall -> startVadCall()
            Intent.StopVadCall -> stopVadCall()
            Intent.DestroyVadCall -> destroyVadCall()
            is Intent.SetEmojiMode -> setEmojiMode(intent.isEmoji)
            is Intent.SendMcpSwitch -> sendMcpSwitch(intent.mcpSwitch)
            Intent.RefreshMessages -> refreshMessages()
            Intent.LoadMoreMessages -> loadMoreMessages()
            Intent.ClearCache -> clearCache()
        }
    }

    // ========== Intent 实现 ==========
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initialize(intent: Intent.Initialize) {
        _uiState.update { it.copy(isLoading = true) }

        this.messageContactItemModel = intent.ao
        this.onReceiveAgentTextCallback = intent.onReceiveAgentTextCallback
        this.onVadChatStateChange = intent.onVadChatStateChange

        intent.ao?.contactId?.toLongOrNull()?.let { agentId ->
            currentAgentId = agentId
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

        intent.initNetworkRunnable.invoke()
        _uiState.update { it.copy(isLoading = false) }
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
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "realtimeChatWsClient::onFailure: ${t.message}")
                _realtimeState.value = RealtimeChatState.Error(t.message ?: "-")
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
        onVadChatStateChange?.onChange(VadChatState.Replying)
        audioController?.startAudioTrackPlay()

        if (currentIsEmoji.get() || isChatCalling?.get() == true) {
            audioController?.stopVAD { Log.d(TAG, "AI正在回复, 停止录音") }
        }
    }

    private fun handleStopTts() {
        _realtimeState.value = RealtimeChatState.InitializedConnected
        onVadChatStateChange?.onChange(VadChatState.Silent)
        audioController?.stopAudioTrackPlay()

        if (currentIsEmoji.get() || isChatCalling?.get() == true) {
            audioController?.startVAD { Log.d(TAG, "AI回复结束, 继续录音") }
        }
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
            onReceiveAgentTextCallback?.onText(response.content)
        }

        // 同步到本地缓存
        syncToLocalCache(response)
    }

    private fun syncToLocalCache(response: com.magicvector.domain.dto.ws.response.WsChatTextResponse) {
        cacheScope.launch {
            // 可选：更新本地数据库
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

    // ========== 音频处理 ==========
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
                onVadChatStateChange?.onChange(VadChatState.Replying)
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
                onVadChatStateChange?.onChange(VadChatState.Speaking)
            }

            override fun speeching(audioBuffer: ByteArray) {
                sendAudioData(audioBuffer)
                onVadChatStateChange?.onChange(VadChatState.Speaking)
            }

            override fun onStopSpeech() {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                realtimeChatWsClient?.sendMessage(dataMap)
                onVadChatStateChange?.onChange(VadChatState.Silent)
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

    private fun startRecordAudio(scope: CoroutineScope) {
        audioController?.startRecordingAudio(isAudioRecording = this, scope)
    }

    private fun stopRecordAudio() {
        _realtimeState.value = RealtimeChatState.InitializedConnected
    }

    private fun startVadCall() {
        audioController?.startVAD(onStart = {
            onVadChatStateChange?.onChange(VadChatState.Silent)
        })
    }

    private fun stopVadCall() {
        audioController?.stopVAD(onStop = {
            onVadChatStateChange?.onChange(VadChatState.Muted)
        })
    }

    private fun destroyVadCall() {
        audioController?.releaseVADController()
        onVadChatStateChange?.onChange(VadChatState.Muted)
    }

    private fun setEmojiMode(isEmoji: Boolean) {
        currentIsEmoji.set(isEmoji)
    }

    override fun isAudioRecording(): Boolean {
        return _realtimeState.value == RealtimeChatState.RecordingAndSending
    }

    fun setHandleSystemResponse(handleSystemResponse: HandleSystemResponse?) {
        this.handleSystemResponse = handleSystemResponse
    }

    fun setCurrentVADStateChange(callback: OnVadChatStateChange) {
        onVadChatStateChange = callback
    }

    fun initIsChatCalling(isCalling: AtomicBoolean) {
        this.isChatCalling = isCalling
    }

    fun reconnectUserConnectionIfNeeded() {
        val userId = userId ?: return
        if (!MainApplication.getNetworkManager().refreshNetworkState().isNetworkOnline) return
        realtimeChatWsClient = initRealtimeChatWsClient()
        startRealtimeWs(forceReconnect = true)
    }

    // ========== 生命周期 ==========
    fun releaseAllResource() {
        currentIsEmoji.set(false)
        isChatCalling = null
        messageContactItemModel = null
        onReceiveAgentTextCallback = null
        _realtimeState.value = RealtimeChatState.NotInitialized

        realtimeChatWsClient?.let {
            it.close()
            realtimeChatWsClient = null
            MainApplication.getNetworkManager().onWebSocketDisconnected(shouldReconnect = false)
        }

        audioController?.releaseAll()
        audioController = null
    }

    fun destroy() {
        releaseAllResource()
        cacheScope.cancel()
    }
}

// ========== State 定义 ==========
data class RealtimeChatUiState(
    val isLoading: Boolean = false,
    val isEmojiMode: Boolean = false,
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

