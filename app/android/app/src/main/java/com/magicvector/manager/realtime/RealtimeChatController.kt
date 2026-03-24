package com.magicvector.manager.realtime

import android.Manifest
import androidx.annotation.RequiresPermission
import com.magicvector.domain.model.mixLLM.McpSwitch
import com.magicvector.domain.constant.VADChatState
import com.magicvector.MainApplication
import com.magicvector.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.magicvector.domain.bo.AgentChatBO
import com.magicvector.manager.audio.IsAudioRecording
import com.magicvector.manager.event.chat.ChatEventManager
import com.magicvector.manager.mcp.HandleSystemResponse
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
    val vadStateEvents: SharedFlow<VADChatState> = eventFlow.vadStateEvents
    val agentTextEvents: SharedFlow<String> = eventFlow.agentTextEvents

    // ========== 数据 ==========
    var agentChatBO: AgentChatBO? = null
    private var agentId: Long? = null
    private var userId: Long? = null
    private var chatEventManager: ChatEventManager? = null

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

        this.agentChatBO = intent.agentChatBo

        agentChatBO?.let { bo ->
            this.agentId = bo.agentId
            chatEventManager = MainApplication.getChatEventMapManager().getOrCreateManager(bo.agentId)
        }

        if (agentChatBO == null) {
            eventFlow.updateRealtimeState(RealtimeChatState.Error("Agent Id is Null"))
            throw IllegalArgumentException("Agent Id is Null")
        }

        // 1. 先初始化所有子模块
        initSubModules()

        // 2. 设置视频帧回调
        visionManager.setOnVideoFrameCallback(intent.onVideoFrame)

        // 3. 初始化 UDP 视觉管理器
        visionManager.init(MainApplication.getUserId(), agentId?.toString() ?: "")

        // 4. 建立 WebSocket 连接
        eventFlow.updateRealtimeState(RealtimeChatState.Initializing)
        val success = webSocketManager.ensureUserConnection(MainApplication.getUserId())
        if (success) {
            bindChannel(agentChatBO!!.agentId)
        }

        // 5. 执行初始化网络回调（加载历史消息）
        intent.initNetworkRunnable.invoke()

        eventFlow.updateUiState { it.copy(isLoading = false) }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initSubModules() {
        // 1. 先创建消息处理器（因为它不依赖其他模块）
        messageHandler = RealtimeChatMessageHandler(
            coroutineScope = coroutineScope,
            eventFlow = eventFlow,
            audioManager = audioManager,
            chatEventManager = chatEventManager,
            agentId = agentId,
            userId = userId
        )

        // 2. 创建 WebSocket 管理器（依赖 messageHandler）
        webSocketManager = RealtimeChatWebSocketManager(eventFlow, messageHandler)

        // 3. 创建音频管理器（依赖 webSocketManager）
        audioManager = RealtimeChatAudioManager(eventFlow, webSocketManager)
        audioManager.initAudioController()

        // 4. 创建视觉管理器
        visionManager = RealtimeChatVisionManager()

        // 5. 创建历史消息管理器
        historyManager = RealtimeChatHistoryManager(coroutineScope, chatEventManager)
    }

    /**
     * 确保用户级别的 WebSocket 连接
     * @param userId 用户ID
     * @return 是否成功建立连接
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun ensureUserConnection(userId: String): Boolean {
        return webSocketManager.ensureUserConnection(userId)
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
        agentChatBO = null
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