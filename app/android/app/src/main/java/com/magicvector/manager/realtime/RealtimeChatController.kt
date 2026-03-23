package com.magicvector.manager

import android.Manifest
import androidx.annotation.RequiresPermission
import com.data.domain.ao.mixLLM.McpSwitch
import com.magicvector.domain.constant.VadChatState
import com.magicvector.MainApplication
import com.magicvector.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.magicvector.domain.model.message.MessageContactItemModel
import com.magicvector.manager.audio.IsAudioRecording
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

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
    }

    // ========== 子模块 ==========
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventFlow = RealtimeChatEventFlow(coroutineScope)

    private lateinit var webSocketManager: RealtimeChatWebSocketManager
    private lateinit var audioManager: RealtimeChatAudioManager
    private lateinit var visionManager: RealtimeChatVisionManager
    private lateinit var historyManager: RealtimeChatHistoryManager
    private lateinit var messageHandler: RealtimeChatMessageHandler

    // ========== 外部可访问的 Flow ==========
    val uiState: StateFlow<RealtimeChatUiState> = eventFlow.uiState
    val realtimeState: StateFlow<RealtimeChatState> = eventFlow.realtimeState
    val vadStateEvents: SharedFlow<VadChatState> = eventFlow.vadStateEvents
    val agentTextEvents: SharedFlow<String> = eventFlow.agentTextEvents

    // ========== 数据 ==========
    var messageContactItemModel: MessageContactItemModel? = null
    private var agentId: Long? = null
    private var userId: Long? = null
    private var chatEventManager: com.magicvector.manager.event.chat.ChatEventManager? = null

    // ========== MVI Intent ==========
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun processIntent(intent: RealtimeChatIntent) {
        when (intent) {
            is RealtimeChatIntent.Initialize -> initialize(intent)
            is RealtimeChatIntent.BindChannel -> bindChannel(intent.agentId)
            is RealtimeChatIntent.SendTextMessage -> sendTextMessage(intent.message)
            RealtimeChatIntent.StartVadCall -> audioManager.startVadCall()
            RealtimeChatIntent.StopVadCall -> audioManager.stopVadCall()
            RealtimeChatIntent.DestroyVadCall -> audioManager.destroyVadCall()
            is RealtimeChatIntent.SendMcpSwitch -> sendMcpSwitch(intent.mcpSwitch)
            RealtimeChatIntent.RefreshMessages -> historyManager.refreshMessages()
            RealtimeChatIntent.LoadMoreMessages -> historyManager.loadMoreMessages()
            RealtimeChatIntent.ClearCache -> historyManager.clearCache()
            is RealtimeChatIntent.SendVideoFrame -> visionManager.sendVideoFrame(intent.bitmap)
        }
    }

    // ========== Intent 实现 ==========
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initialize(intent: RealtimeChatIntent.Initialize) {
        eventFlow.updateUiState { it.copy(isLoading = true) }

        this.messageContactItemModel = intent.ao
        visionManager.setOnVideoFrameCallback(intent.onVideoFrame)

        intent.ao?.contactId?.toLongOrNull()?.let { agentId ->
            this.agentId = agentId
            chatEventManager = MainApplication.getChatEventMapManager().getOrCreateManager(agentId)
        }

        intent.chatAAo.nameLd.postValue(intent.ao?.vo?.name ?: "")
        intent.chatAAo.avatarUrlLd.postValue(intent.ao?.vo?.avatarUrl ?: "")

        if (messageContactItemModel?.contactId != null) {
            eventFlow.updateRealtimeState(RealtimeChatState.Initializing)

            // 初始化子模块
            initSubModules()

            // 建立用户连接
            val success = webSocketManager.ensureUserConnection(MainApplication.getUserId())
            if (success) {
                bindChannel(messageContactItemModel!!.contactId!!.toLong())
            }
        } else {
            eventFlow.updateRealtimeState(RealtimeChatState.Error("Agent Id is Null"))
            throw IllegalArgumentException("Agent Id is Null")
        }

        // 初始化 UDP 视觉管理器
        visionManager.init(MainApplication.getUserId(), agentId?.toString() ?: "")

        intent.initNetworkRunnable.invoke()
        eventFlow.updateUiState { it.copy(isLoading = false) }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initSubModules() {
        // 消息处理器（先创建，但需要等 webSocketManager 和 audioManager 初始化后再设置）
        messageHandler = RealtimeChatMessageHandler(
            coroutineScope = coroutineScope,
            eventFlow = eventFlow,
            audioManager = audioManager,
            chatEventManager = chatEventManager,
            agentId = agentId,
            userId = userId
        )

        // WebSocket 管理器
        webSocketManager = RealtimeChatWebSocketManager(eventFlow, messageHandler)

        // 音频管理器
        audioManager = RealtimeChatAudioManager(eventFlow, webSocketManager)
        audioManager.initAudioController()

        // 视觉管理器
        visionManager = RealtimeChatVisionManager()

        // 历史消息管理器
        historyManager = RealtimeChatHistoryManager(coroutineScope, chatEventManager)
    }

    private fun bindChannel(agentId: Long) {
        this.agentId = agentId
        webSocketManager.bindChannel(agentId)
    }

    private fun sendTextMessage(message: String) {
        webSocketManager.sendTextMessage(message)
    }

    private fun sendMcpSwitch(mcpSwitch: McpSwitch) {
        val mcpSwitchJson = MainApplication.GSON.toJson(mcpSwitch)
        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to mcpSwitchJson
        )
        webSocketManager.sendSystemMessage(dataMap)
    }

    // ========== 公共方法 ==========
    fun setHandleSystemResponse(handleSystemResponse: HandleSystemResponse?) {
        messageHandler.handleSystemResponse = handleSystemResponse
    }

    fun reconnectUserConnectionIfNeeded() {
        webSocketManager.reconnectIfNeeded()
    }

    override fun isAudioRecording(): Boolean {
        return audioManager.isAudioRecording()
    }

    // ========== 生命周期 ==========
    fun releaseAllResource() {
        messageContactItemModel = null
        eventFlow.updateRealtimeState(RealtimeChatState.NotInitialized)

        webSocketManager.release()
        audioManager.release()
        visionManager.release()
    }

    fun destroy() {
        releaseAllResource()
        coroutineScope.cancel()
    }
}