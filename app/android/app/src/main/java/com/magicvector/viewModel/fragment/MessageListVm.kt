package com.magicvector.viewModel.fragment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.model.message.MessageContactItemModel
import com.magicvector.MainApplication
import com.magicvector.domain.bo.AgentChatBO
import com.magicvector.domain.event.EventSource
import com.magicvector.domain.vo.agent.AgentVO
import com.magicvector.manager.network.NetworkState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessageListMviVm : ViewModel() {

    companion object {
        val TAG: String = MessageListMviVm::class.java.name
        // ========== Managers（数据源） ==========
        val api = MainApplication.getApiRequestImplInstance()
        val agentEventManager = MainApplication.getAgentEventManager()
    }

    // ========== UI 直接观察的数据流（从 Manager 暴露） ==========
    val agents: StateFlow<List<AgentChatModel>> = agentEventManager.items
    // ========== UI 状态（不包含列表数据） ==========
    private val _uiState = MutableStateFlow(MessageListUiState())
    val uiState: StateFlow<MessageListUiState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(MessageListDataState())
    val dataState: StateFlow<MessageListDataState> = _dataState.asStateFlow()

    private val _effect = Channel<MessageListEffect>(Channel.BUFFERED)
    val effect: Flow<MessageListEffect> = _effect.receiveAsFlow()
    private var lastObservedNetworkState: NetworkState = MainApplication.getNetworkManager().state.value

    init {
        observeNetworkState()
        observeManagerEvents()
    }

    // ========== Intent 处理 ==========
    fun processIntent(intent: MessageListIntent) {
        when (intent) {
            MessageListIntent.Initialize -> initialize()
            is MessageListIntent.SelectAgent -> onAgentClick(intent.position)
            is MessageListIntent.EditAgent -> onEditAgent(intent.position)
            MessageListIntent.CreateAgent -> sendEffect(MessageListEffect.OpenCreateAgent)
            MessageListIntent.Refresh -> refresh()
            is MessageListIntent.UpdateConnectionState -> {
                _dataState.update {
                    it.copy(
                        isServiceBound = intent.isServiceBound,
                        isWsConnected = intent.isWsConnected
                    )
                }
            }
            is MessageListIntent.AgentCreated -> {
                if (intent.created) {
                    refresh()
                }
            }
            MessageListIntent.StartChat -> sendEffect(MessageListEffect.RequestAudioPermission)
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            // 更新网络状态
            val latestNetworkState = MainApplication.getNetworkManager().refreshNetworkState()
            _dataState.update {
                it.copy(
                    isNetworkOnline = latestNetworkState.isNetworkOnline,
                    isWsConnected = latestNetworkState.isWsConnected
                )
            }

            val userId = resolveUserId().toLongOrNull()
            if (userId == null || userId <= 0L) {
                Log.w(TAG, "User not logged in")
                return@launch
            }

            _dataState.update { it.copy(hasInitialized = true) }

            // 检查是否有数据
            val hasAgents = agentEventManager.items.value.isNotEmpty()

            if (hasAgents) {
                // 已有数据，更新 UI 模式
                updateUiMode()
                finishSync(hasException = false)
            } else if (latestNetworkState.isNetworkOnline) {
                // 无数据且有网，刷新
                refresh()
            } else {
                // 无数据且无网，尝试从本地加载（Manager 会自动处理）
                finishSync(hasException = false)
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, isLoading = true) }

            val userId = resolveUserId()
            if (userId.isEmpty()) {
                handleError("用户未登录")
                return@launch
            }

            // 有网络时调用 Http 全量加载（Manager 内部会处理网络请求和数据更新）
            if (_dataState.value.isNetworkOnline) {
                try {
                    agentEventManager.onHttpFullLoad()
                    // ChatManager 也需要刷新，但需要先获取 Agent 列表后的消息？
                    // 这里根据你的业务逻辑决定是否一起刷新
                    finishSync(hasException = false)
                } catch (e: Exception) {
                    Log.e(TAG, "Refresh failed", e)
                    handleError("刷新失败: ${e.message}")
                    finishSync(hasException = true)
                }
            } else {
                // 无网络时从本地加载（Manager 内部会自动处理）
                agentEventManager.onLocalFullLoad()
                finishSync(hasException = false)
            }
        }
    }

    private fun updateUiMode() {
        val hasAgent = agentEventManager.items.value.isNotEmpty()

        _dataState.update {
            it.copy(
                hasAgent = hasAgent,
            )
        }

        _uiState.update {
            it.copy(
                uiMode = deriveUiMode(hasAgent)
            )
        }
    }

    // ========== 监听网络状态 ==========
    private fun observeNetworkState() {
        viewModelScope.launch {
            MainApplication.getNetworkManager().state.collect { networkState ->
                val previous = lastObservedNetworkState
                lastObservedNetworkState = networkState
                _dataState.update {
                    it.copy(
                        isNetworkOnline = networkState.isNetworkOnline,
                        isWsConnected = networkState.isWsConnected
                    )
                }
                val userId = resolveUserId().toLongOrNull() ?: return@collect
                // 网络恢复: 重新同步数据
                if (!previous.isNetworkOnline && networkState.isNetworkOnline) {
                    refresh()
                }
                // 断网: 调用本地数据
                else if (previous.isNetworkOnline && !networkState.isNetworkOnline) {
                    finishSync(hasException = false)
                }
            }
        }
    }

    // ========== 监听 Manager 事件（用于持久化决策） ==========
    private fun observeManagerEvents() {
        viewModelScope.launch {
            // 监听 AgentManager 事件
            agentEventManager.events.collect { event ->
                // 根据事件来源决定是否持久化到 Room
                when (event) {
                    is EventSource.Remote,
                    is EventSource.UserAction,
                    is EventSource.WebSocket -> {
                        // 非 Room 来源的数据变化，需要持久化
                        // Agent 的持久化逻辑可以在这里处理
                        Log.d(TAG, "Agent event from: $event, should sync to Room")
                    }
                    else -> {
                        // Room 来源的事件，不需要再持久化
                        Log.d(TAG, "Agent event from Room, skip sync")
                    }
                }
            }
        }

        viewModelScope.launch {
            // 监听 ChatManager 事件
            agentEventManager.events.collect { event ->
                when (event) {
                    is EventSource.Remote,
                    is EventSource.UserAction,
                    is EventSource.WebSocket -> {
                        // 非 Room 来源的数据变化，需要持久化
                        Log.d(TAG, "Chat event from: $event, should sync to Room")
                    }
                    else -> {
                        Log.d(TAG, "Chat event from Room, skip sync")
                    }
                }
            }
        }
    }

    private fun onAgentClick(position: Int) {
        val agentList = agents.value
        if (agentList.size > position) {
            val agent = agentList[position]
            sendEffect(MessageListEffect.NavigateToChat(
                bo = AgentChatBO(
                    agentId = agent.agentId,
                    agentVo = agent.agentChatVo?.agentVo?: AgentVO(),
                )
            ))
        }
    }

    private fun onEditAgent(position: Int) {
        val agentList = agents.value
        if (agentList.size > position) {
            val agentId = agentList[position].agentId
            sendEffect(MessageListEffect.OpenAgentEditor(agentId))
        }
    }

    private fun sendEffect(effect: MessageListEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private suspend fun resolveUserId(): String {
        val cached = MainApplication.getUserId()
        if (cached.isNotBlank()) {
            return cached
        }
        val localUser = MainApplication.getUserManager().getCurrentUser()
        val localUserId = localUser?.userId ?: 0L
        if (localUserId > 0L) {
            MainApplication.updateUserId(localUserId)
            return localUserId.toString()
        }
        return ""
    }

    private fun finishSync(hasException: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                uiMode = deriveUiMode(
                    hasAgent = _dataState.value.hasAgent,
                )
            )
        }
        _dataState.update { it.copy(hasException = hasException) }
    }

    private fun handleError(error: String) {
        Log.e(TAG, "handleError: $error")
        _dataState.update { it.copy(hasException = true) }
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                error = error
            )
        }
        sendEffect(MessageListEffect.ShowToast(error))
    }

    private fun deriveUiMode(hasAgent: Boolean): MessageListUiMode {
        return when {
            !hasAgent -> MessageListUiMode.NO_AGENT
            else -> MessageListUiMode.HAS_AGENT
        }
    }
}

sealed class MessageListIntent {
    data object Initialize : MessageListIntent()
    data class SelectAgent(val position: Int) : MessageListIntent()
    data class EditAgent(val position: Int) : MessageListIntent()
    data object CreateAgent : MessageListIntent()
    data object Refresh : MessageListIntent()
    data class UpdateConnectionState(
        val isServiceBound: Boolean,
        val isWsConnected: Boolean
    ) : MessageListIntent()
    data class AgentCreated(val created: Boolean) : MessageListIntent()
    data object StartChat : MessageListIntent()
}

data class MessageListDataState(
    val isServiceBound: Boolean = false,
    val isNetworkOnline: Boolean = true,
    val isWsConnected: Boolean = false,
    val hasInitialized: Boolean = false,
    val hasException: Boolean = false,
    val hasAgent: Boolean = false,
)

data class MessageListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val uiMode: MessageListUiMode = MessageListUiMode.NO_AGENT,
    val error: String? = null,
    val isFirstOpen: Boolean = true
)

enum class MessageListUiMode {
    NO_AGENT,
    HAS_AGENT
}

sealed class MessageListEffect {
    data object OpenCreateAgent : MessageListEffect()
    data class OpenAgentEditor(val agentId: Long) : MessageListEffect()
    data class NavigateToChat(val bo: AgentChatBO) : MessageListEffect()
    data class ShowToast(val message: String) : MessageListEffect()
    data object RequestAudioPermission : MessageListEffect()
}
