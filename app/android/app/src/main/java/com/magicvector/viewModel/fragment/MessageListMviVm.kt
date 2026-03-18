package com.magicvector.viewModel.fragment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.ao.agent.AgentAo
import com.data.domain.ao.agent.AgentChatAo
import com.data.domain.ao.message.MessageContactItemAo
import com.magicvector.MainApplication
import com.magicvector.domain.convertor.MessageConvertor
import com.magicvector.domain.exception.NetworkBusinessException
import com.magicvector.manager.network.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class MessageListMviVm : ViewModel() {

    companion object {
        val TAG: String = MessageListMviVm::class.java.name
        val api = MainApplication.getApiRequestImplInstance()
    }

    private val _uiState = MutableStateFlow(MessageListState())
    val uiState: StateFlow<MessageListState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(MessageListDataState())
    val dataState: StateFlow<MessageListDataState> = _dataState.asStateFlow()

    private val _effect = Channel<MessageListEffect>(Channel.BUFFERED)
    val effect: Flow<MessageListEffect> = _effect.receiveAsFlow()
    private var lastObservedNetworkState: NetworkState = MainApplication.getNetworkManager().state.value

    init {
        observeNetworkState()
    }

    fun processIntent(intent: MessageListIntent) {
        when (intent) {
            MessageListIntent.Initialize -> initialize()
            is MessageListIntent.SelectMessage -> onMessageItemClick(intent.position)
            is MessageListIntent.EditAgent -> onEditAgent(intent.position)
            MessageListIntent.CreateAgent -> sendEffect(MessageListEffect.OpenCreateAgent)
            MessageListIntent.Refresh -> refreshMessages()
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
                    refreshMessages()
                }
            }
            MessageListIntent.StartChat -> sendEffect(MessageListEffect.RequestAudioPermission)
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            val latestNetworkState = MainApplication.getNetworkManager().refreshNetworkState()
            _dataState.update {
                it.copy(
                    isNetworkOnline = latestNetworkState.isNetworkOnline,
                    isWsConnected = latestNetworkState.isWsConnected
                )
            }
            val userId = resolveUserId().toLongOrNull() ?: return@launch
            if (userId <= 0L) {
                return@launch
            }
            if (_dataState.value.hasInitialized) {
                applyManagerSnapshot(userId)
                return@launch
            }
            _dataState.update { it.copy(hasInitialized = true) }
            val hasManagerCache = applyManagerSnapshot(userId)
            if (latestNetworkState.isNetworkOnline) {
                refreshMessages(showLoading = !hasManagerCache && _uiState.value.messages.isEmpty())
            } else {
                finishSync(hasException = false)
            }
        }
    }

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
                    refreshMessages(showLoading = _uiState.value.messages.isEmpty())
                }
                // 断网: 调用本地数据
                else if (previous.isNetworkOnline && !networkState.isNetworkOnline) {
                    applyCachedSnapshot(userId)
                    finishSync(hasException = false)
                }
            }
        }
    }

    private fun refreshMessages(showLoading: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, isLoading = showLoading) }
            syncAgentAndMessageState()
        }
    }

    private fun onMessageItemClick(position: Int) {
        val messages = _uiState.value.messages
        if (messages.size > position) {
            sendEffect(MessageListEffect.NavigateToChat(messages[position]))
        }
    }

    private fun onEditAgent(position: Int) {
        val messages = _uiState.value.messages
        if (messages.size > position) {
            val agentId = messages[position].contactId
            if (!agentId.isNullOrBlank()) {
                sendEffect(MessageListEffect.OpenAgentEditor(agentId))
            }
        }
    }

    private fun sendEffect(effect: MessageListEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    fun initNetworkRequest() {
        refreshMessages(showLoading = _uiState.value.messages.isEmpty())
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

    private suspend fun syncAgentAndMessageState() {
        val userId = resolveUserId()
        val parsedUserId = userId.toLongOrNull()
        if (userId.isBlank() || parsedUserId == null || parsedUserId <= 0L) {
            handleError("用户未登录")
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }

        // 一开始就没网络
        if (!MainApplication.getNetworkManager().state.value.isNetworkOnline) {
            // 同步本地数据
            applyCachedSnapshot(parsedUserId)
            finishSync(hasException = false)
            return
        }

        // 有网络：全量请求
        runCatching {
            supervisorScope {
                val agentDeferred = async(Dispatchers.IO) { api.getAgentList(userId).agentAos.orEmpty() }
                val chatDeferred = async(Dispatchers.IO) { api.getLastAgentChatList(userId).agentChatAos.orEmpty() }
                OnlineHomeSnapshot(
                    agents = agentDeferred.await(),
                    agentChats = chatDeferred.await()
                )
            }
        }.onSuccess { snapshot ->
            applyOnlineSnapshot(parsedUserId, snapshot)
            finishSync(hasException = false)
        }.onFailure { exception ->
            Log.e(TAG, "远端同步失败，回退本地缓存", exception)
            val loaded = applyCachedSnapshot(parsedUserId)
            val message = when (exception) {
                is NetworkBusinessException -> exception.message ?: "远端同步失败"
                else -> "远端同步失败: ${exception.message}"
            }
            if (!loaded) {
                handleError(message)
            } else {
                _uiState.update { it.copy(error = message) }
                _dataState.update { it.copy(hasException = true) }
                sendEffect(MessageListEffect.ShowToast("$message，已回退到本地缓存"))
            }
            finishSync(hasException = true)
        }
    }

    private suspend fun applyOnlineSnapshot(userId: Long, snapshot: OnlineHomeSnapshot) {
        val messageItems = MessageConvertor.agentChatAos2MessageContactItemAos(snapshot.agentChats)
            .sortedByDescending { it.timestamp }
        // 离线同步
        MainApplication.getChatCacheManager().syncHomeSnapshot(
            userId = userId,
            agents = snapshot.agents,
            agentChats = snapshot.agentChats
        )
        MainApplication.getAgentsManager().setAgents(snapshot.agents)
        MainApplication.getMessageListManager().setMessageContactItemAos(messageItems)
        // ui更新
        applyUiSnapshot(
            agentCount = snapshot.agents.size,
            messages = messageItems,
            error = null
        )
    }

    private suspend fun applyCachedSnapshot(userId: Long): Boolean {
        val snapshot = MainApplication.getChatCacheManager().queryHomeSnapshot(userId)
        MainApplication.getAgentsManager().setAgents(snapshot.agents)
        MainApplication.getMessageListManager().setMessageContactItemAos(snapshot.messageItems)
        applyUiSnapshot(
            agentCount = snapshot.agents.size,
            messages = snapshot.messageItems,
            error = null
        )
        return snapshot.agents.isNotEmpty() || snapshot.messageItems.isNotEmpty()
    }

    private suspend fun applyManagerSnapshot(userId: Long): Boolean {
        val agents = MainApplication.getAgentsManager().agentList.value
        val messages = MainApplication.getMessageListManager().messageContactItemAos.toList()
            .sortedByDescending { it.timestamp }
        if (agents.isNotEmpty() || messages.isNotEmpty()) {
            applyUiSnapshot(
                agentCount = agents.size,
                messages = messages,
                error = null
            )
            return true
        }
        return applyCachedSnapshot(userId)
    }

    private fun applyUiSnapshot(
        agentCount: Int,
        messages: List<MessageContactItemAo>,
        error: String?
    ) {
        val hasAgent = agentCount > 0
        val hasMessage = messages.isNotEmpty()
        _dataState.update {
            it.copy(
                hasAgent = hasAgent,
                hasMessage = hasMessage
            )
        }
        _uiState.update {
            it.copy(
                agentCount = agentCount,
                messages = messages,
                messageCount = messages.size,
                error = error,
                uiMode = deriveUiMode(
                    hasAgent = hasAgent,
                    hasMessage = hasMessage
                )
            )
        }
    }

    private fun finishSync(hasException: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                uiMode = deriveUiMode(
                    hasAgent = _dataState.value.hasAgent,
                    hasMessage = _dataState.value.hasMessage
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

    private fun deriveUiMode(hasAgent: Boolean, hasMessage: Boolean): MessageListUiMode {
        return when {
            !hasAgent -> MessageListUiMode.NO_AGENT
            !hasMessage -> MessageListUiMode.HAS_AGENT_NO_MESSAGE
            else -> MessageListUiMode.HAS_MESSAGE
        }
    }
}

sealed class MessageListIntent {
    data object Initialize : MessageListIntent()
    data class SelectMessage(val position: Int) : MessageListIntent()
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
    val hasMessage: Boolean = false
)

data class MessageListState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val messages: List<MessageContactItemAo> = emptyList(),
    val messageCount: Int = 0,
    val agentCount: Int = 0,
    val uiMode: MessageListUiMode = MessageListUiMode.NO_AGENT,
    val error: String? = null,
    val isFirstOpen: Boolean = true,
    val needLoadNetworkData: Boolean = false
)

enum class MessageListUiMode {
    NO_AGENT,
    HAS_AGENT_NO_MESSAGE,
    HAS_MESSAGE
}

sealed class MessageListEffect {
    data object OpenCreateAgent : MessageListEffect()
    data class OpenAgentEditor(val agentId: String) : MessageListEffect()
    data class NavigateToChat(val ao: MessageContactItemAo) : MessageListEffect()
    data class ShowToast(val message: String) : MessageListEffect()
    data object RequestAudioPermission : MessageListEffect()
}

private data class OnlineHomeSnapshot(
    val agents: List<AgentAo>,
    val agentChats: List<AgentChatAo>
)
