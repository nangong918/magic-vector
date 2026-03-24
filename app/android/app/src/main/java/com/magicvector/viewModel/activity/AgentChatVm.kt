package com.magicvector.viewModel.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Stable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.MainApplication
import com.magicvector.domain.bo.AgentChatBO
import com.magicvector.domain.constant.VADChatState
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.manager.realtime.RealtimeChatController
import com.magicvector.manager.event.chat.ChatEventManager
import com.magicvector.manager.network.NetworkState
import com.magicvector.manager.realtime.RealtimeChatIntent
import com.magicvector.manager.realtime.RealtimeChatState
import com.view.appview.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgentChatVm : ViewModel() {

    companion object {
        val TAG: String = AgentChatVm::class.java.name
        val application = MainApplication.getApp()
        private val chatEventMapManager = MainApplication.getChatEventMapManager()
    }

    // ========== Managers ==========
    private var agentId: Long = 0L
    private val chatEventManager: ChatEventManager?
        get() = if (agentId > 0) chatEventMapManager.getOrCreateManager(agentId) else null

    private var realtimeChatController: RealtimeChatController? = null

    // ========== UI 直接观察的数据流 ==========
    val messages: StateFlow<List<ChatMessageModel>> = chatEventManager?.items ?:
        MutableStateFlow<List<ChatMessageModel>>(emptyList()).asStateFlow()

    // ========== UI 状态 ==========
    private val _uiState = MutableStateFlow(AgentChatUiState())
    val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

    private val _effect = Channel<AgentChatEffect>(Channel.BUFFERED)
    val effect: Flow<AgentChatEffect> = _effect.receiveAsFlow()

    private var lastObservedNetworkState: NetworkState =
        MainApplication.getNetworkManager().state.value

    init {
        observeNetworkState()
        observeRealtimeControllerEvents()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun processIntent(intent: AgentChatIntent) {
        when (intent) {
            is AgentChatIntent.Initialize -> initialize(intent.intent, intent.activity)
            AgentChatIntent.Resume -> Unit // 新架构中不需要单独 Resume
            is AgentChatIntent.SendTextMessage -> sendTextMessage(intent.message)
            is AgentChatIntent.StartSendVoice -> startSendVoice(intent.scope)
            AgentChatIntent.StopSendVoice -> stopSendVoice()
            AgentChatIntent.RequestCall -> sendEffect(AgentChatEffect.RequestRecordPermission)
            is AgentChatIntent.CallPermissionGranted -> openVoiceMode(intent.context)
            AgentChatIntent.CallPermissionDenied -> {
                sendEffect(AgentChatEffect.ShowToastRes(R.string.permission_denied))
            }
            AgentChatIntent.ToggleMic -> toggleMicState()
            AgentChatIntent.EndVoiceMode -> endVoiceMode()
            is AgentChatIntent.PageChanged -> _uiState.update { it.copy(currentPage = intent.page) }
            AgentChatIntent.SwitchToEmojiPage -> _uiState.update { it.copy(currentPage = 0) }
            is AgentChatIntent.SendVideoFrame -> sendVideoFrame(intent.bitmap)
        }
    }

    // ========== 初始化 ==========
    /**
     * 初始化页面与会话资源
     * @param intent 包含 AgentChatBO 的 Intent
     * @param activity 当前 FragmentActivity
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initialize(intent: Intent, activity: FragmentActivity) {
        val agentChatBo = try {
            intent.getSerializableExtra(AgentChatBO::class.simpleName) as AgentChatBO
        } catch (e: Exception) {
            Log.e(TAG, "ComposeAgentChatActivity::intentAo转换失败", e)
            sendEffect(AgentChatEffect.Finish)
            return
        }

        agentId = agentChatBo.agentId
        _uiState.update {
            it.copy(
                title = agentChatBo.agentVo.name,
                avatarUrl = agentChatBo.agentVo.avatarUrl,
            )
        }

        // 初始化 RealtimeChatController
        realtimeChatController = RealtimeChatController()

        // 发送初始化 Intent
        realtimeChatController?.processIntent(
            RealtimeChatIntent.Initialize(
                chatActivity = activity,
                agentChatBo = agentChatBo,
                initNetworkRunnable = {
                    viewModelScope.launch {
                        refreshMessages()
                    }
                },
                onVideoFrame = { bitmap ->
                    // 可选：处理 YOLOv8 识别结果
                    _uiState.update { it.copy(lastFrame = bitmap) }
                }
            )
        )

        // 绑定 Agent 频道
        realtimeChatController?.processIntent(RealtimeChatIntent.BindChannel(agentId))

        // 启动 VAD 检测（进入页面即开始）
        realtimeChatController?.processIntent(RealtimeChatIntent.StartVadCall)

        // 刷新消息
        viewModelScope.launch {
            refreshMessages()
        }
    }

    // ========== 观察 RealtimeChatController 的事件 ==========
    /**
     * 监听实时聊天控制器的事件流
     * - vadStateEvents: VAD 状态变化 → 更新 UI 中的球体动画
     * - agentTextEvents: Agent 文本回复 → 更新流式文本显示
     * - realtimeState: 连接状态 → 更新发送按钮可用状态
     * - uiState: 控制器内部 UI 状态 → 更新加载状态
     */
    private fun observeRealtimeControllerEvents() {
        val controller = realtimeChatController ?: return

        viewModelScope.launch {
            controller.vadStateEvents.collect { vadState ->
                val actual = if (_uiState.value.isMicClosed &&
                    (vadState is VADChatState.Silent || vadState is VADChatState.Speaking)
                ) {
                    VADChatState.Muted
                } else {
                    vadState
                }
                _uiState.update {
                    it.copy(
                        vadChatState = actual,
                        orbPhase = mapOrbPhase(actual, controller.realtimeState.value),
                        orbExpanded = (actual is VADChatState.Speaking || actual is VADChatState.Replying)
                    )
                }
            }
        }

        viewModelScope.launch {
            controller.agentTextEvents.collect { text ->
                _uiState.update { it.copy(agentText = text) }
            }
        }

        viewModelScope.launch {
            controller.realtimeState.collect { state ->
                val enableSend = when (state) {
                    is RealtimeChatState.InitializedConnected -> true
                    is RealtimeChatState.RecordingAndSending -> true
                    else -> false
                }
                _uiState.update {
                    it.copy(
                        isEnableSend = enableSend,
                        isChatServiceBound = state is RealtimeChatState.InitializedConnected,
                        orbPhase = mapOrbPhase(_uiState.value.vadChatState, state)
                    )
                }
            }
        }

        viewModelScope.launch {
            controller.uiState.collect { uiState ->
                _uiState.update { it.copy(isLoading = uiState.isLoading) }
            }
        }
    }

    // ========== 消息管理 ==========
    /**
     * 刷新消息列表（网络请求）
     */
    private suspend fun refreshMessages() {
        val manager = chatEventManager ?: return

        _uiState.update { it.copy(isLoading = true) }
        try {
            val latestNetworkState = MainApplication.getNetworkManager().refreshNetworkState()
            if (!latestNetworkState.isNetworkOnline) {
                loadMessagesFromLocal()
                return
            }

            // 使用新的 ChatEventManager 加载数据
            manager.onHttpFullLoad()
        } catch (e: Exception) {
            Log.e(TAG, "refreshMessages failed", e)
            loadMessagesFromLocal()
            sendEffect(AgentChatEffect.ShowToast("聊天记录同步失败，已回退到本地缓存"))
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 从本地数据库加载消息（离线模式）
     */
    private suspend fun loadMessagesFromLocal() {
        val manager = chatEventManager ?: return
        manager.onLocalFullLoad()
    }


    // ========== 发送消息 ==========
    /**
     * 发送文本消息
     * @param message 消息内容
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun sendTextMessage(message: String) {
        val isAllWhitespaceOrSpecialChars = message.all { it.isWhitespace() || !it.isLetterOrDigit() }
        if (message.isBlank() || isAllWhitespaceOrSpecialChars) {
            sendEffect(AgentChatEffect.ShowToastRes(R.string.please_input_legal_content))
            return
        }

        realtimeChatController?.processIntent(RealtimeChatIntent.SendTextMessage(message))
            ?: sendEffect(AgentChatEffect.ShowToast("连接未建立，发送失败"))
    }

    // ========== 视频帧发送 ==========
    /**
     * 发送视频帧（用于 YOLOv8 物体检测）
     * @param bitmap 视频帧图片
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun sendVideoFrame(bitmap: Bitmap) {
        realtimeChatController?.processIntent(RealtimeChatIntent.SendVideoFrame(bitmap))
    }

    // ========== 音频控制 ==========
    /**
     * 开始录音发送（VAD 持续录音，此方法保留兼容性）
     */
    private fun startSendVoice(scope: CoroutineScope) {
        // 新架构中不需要单独 startSendVoice，VAD 持续录音
        // 但为了兼容旧接口，保留空实现
    }

    /**
     * 停止录音发送（VAD 持续录音，此方法保留兼容性）
     */
    private fun stopSendVoice() {
        // 新架构中不需要单独 stopSendVoice，VAD 自动处理
    }

    /**
     * 打开语音模式（VAD 持续录音，此方法保留兼容性）
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun openVoiceMode(context: Context) {
        // 新架构中进入页面即开始 VAD，不需要单独打开
        _uiState.update {
            it.copy(
                isMicClosed = false,
                vadChatState = VADChatState.Silent,
                orbPhase = AgentVoiceOrbPhase.READY
            )
        }
    }

    /**
     * 切换麦克风状态
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun toggleMicState() {
        val current = _uiState.value
        if (current.isMicClosed) {
            realtimeChatController?.processIntent(RealtimeChatIntent.StartVadCall)
            _uiState.update { it.copy(isMicClosed = false, vadChatState = VADChatState.Silent) }
        } else {
            realtimeChatController?.processIntent(RealtimeChatIntent.StopVadCall)
            _uiState.update { it.copy(isMicClosed = true, vadChatState = VADChatState.Muted) }
        }
    }

    /**
     * 结束语音模式
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun endVoiceMode() {
        realtimeChatController?.processIntent(RealtimeChatIntent.DestroyVadCall)
        _uiState.update {
            it.copy(
                isMicClosed = true,
                vadChatState = VADChatState.Muted,
                agentText = "",
                orbExpanded = false,
                orbPhase = AgentVoiceOrbPhase.DISCONNECTED
            )
        }
    }

    // ========== 网络状态监听 ==========
    /**
     * 监听网络状态变化
     * - 网络恢复：重新同步消息 + 重连 WebSocket
     * - 断网：从本地加载消息
     */
    private fun observeNetworkState() {
        viewModelScope.launch {
            MainApplication.getNetworkManager().state.collect { networkState ->
                val previous = lastObservedNetworkState
                lastObservedNetworkState = networkState

                if (agentId == 0L) return@collect

                if (!previous.isNetworkOnline && networkState.isNetworkOnline) {
                    refreshMessages()
                    realtimeChatController?.reconnectUserConnectionIfNeeded()
                } else if (previous.isNetworkOnline && !networkState.isNetworkOnline) {
                    loadMessagesFromLocal()
                }
            }
        }
    }

    // ========== 辅助方法 ==========
    /**
     * 映射底部状态球阶段
     * @param vadState VAD 状态
     * @param realtimeState 实时连接状态
     * @return 球体阶段
     */
    private fun mapOrbPhase(
        vadState: VADChatState,
        realtimeState: RealtimeChatState?
    ): AgentVoiceOrbPhase {
        if (realtimeState is RealtimeChatState.Error || vadState is VADChatState.Error) {
            return AgentVoiceOrbPhase.ERROR
        }
        if (realtimeState !is RealtimeChatState.InitializedConnected &&
            realtimeState !is RealtimeChatState.RecordingAndSending &&
            realtimeState !is RealtimeChatState.Receiving
        ) {
            return AgentVoiceOrbPhase.DISCONNECTED
        }
        return when (vadState) {
            is VADChatState.Speaking -> AgentVoiceOrbPhase.USER_SPEAKING
            is VADChatState.Replying -> AgentVoiceOrbPhase.AGENT_REPLYING
            else -> AgentVoiceOrbPhase.READY
        }
    }


    /**
     * 发送副作用事件
     */
    private fun sendEffect(effect: AgentChatEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeChatController?.destroy()
        realtimeChatController = null
    }
}

// ========== UI State ==========
@Stable
data class AgentChatUiState(
    /** 顶部标题 */
    val title: String = "",
    /** 顶部头像 */
    val avatarUrl: String? = null,
    /** 页面级加载态。 */
    val isLoading: Boolean = false,
    /** 发送区可用态 */
    val isEnableSend: Boolean = false,
    /** ChatService 绑定状态 */
    val isChatServiceBound: Boolean = false,
    /** 当前页面索引（0: Emoji，1: Text） */
    val currentPage: Int = 0,
    /** 麦克风是否关闭 */
    val isMicClosed: Boolean = true,
    /** 当前 VAD 状态 */
    val vadChatState: VADChatState = VADChatState.Muted,
    /** Agent 回复文本（流式片段） */
    val agentText: String = "",
    /** 底部状态球阶段 */
    val orbPhase: AgentVoiceOrbPhase = AgentVoiceOrbPhase.DISCONNECTED,
    /** 底部状态球弹性缩放开关 */
    val orbExpanded: Boolean = false,
    /** 最新视频帧（用于调试） */
    val lastFrame: Bitmap? = null
)

enum class AgentVoiceOrbPhase {
    DISCONNECTED,
    ERROR,
    READY,
    USER_SPEAKING,
    AGENT_REPLYING
}

// ========== Intent ==========
sealed class AgentChatIntent {
    /** 初始化页面与会话资源。 */
    data class Initialize(val intent: Intent, val activity: FragmentActivity) : AgentChatIntent()

    /** 生命周期恢复。 */
    data object Resume : AgentChatIntent()

    /** 文本发送。 */
    data class SendTextMessage(val message: String) : AgentChatIntent()

    /** 开始录音发送。 */
    data class StartSendVoice(val scope: CoroutineScope) : AgentChatIntent()

    /** 结束录音发送。 */
    data object StopSendVoice : AgentChatIntent()

    /** 请求进入语音唤醒模式。 */
    data object RequestCall : AgentChatIntent()

    /** 录音权限通过。 */
    data class CallPermissionGranted(val context: Context) : AgentChatIntent()

    /** 录音权限拒绝。 */
    data object CallPermissionDenied : AgentChatIntent()

    /** 切换 Mic 开关。 */
    data object ToggleMic : AgentChatIntent()

    /** 结束语音模式。 */
    data object EndVoiceMode : AgentChatIntent()

    /** Pager 页面变化。 */
    data class PageChanged(val page: Int) : AgentChatIntent()

    /** 切换到 Emoji 页面。 */
    data object SwitchToEmojiPage : AgentChatIntent()

    /** 发送视频帧（YOLOv8 识别） */
    data class SendVideoFrame(val bitmap: Bitmap) : AgentChatIntent()
}

// ========== Effect ==========
sealed class AgentChatEffect {
    data object Finish : AgentChatEffect()
    data object RequestRecordPermission : AgentChatEffect()
    data class ShowToast(val message: String) : AgentChatEffect()
    data class ShowToastRes(val messageRes: Int) : AgentChatEffect()
}