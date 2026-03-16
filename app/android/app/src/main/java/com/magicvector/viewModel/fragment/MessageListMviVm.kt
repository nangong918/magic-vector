package com.magicvector.viewModel.fragment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.ao.message.MessageContactItemAo
import com.magicvector.MainApplication
import com.magicvector.domain.convertor.MessageConvertor
import com.magicvector.domain.exception.NetworkBusinessException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class MessageListMviVm : ViewModel() {

    companion object {
        val TAG: String = MessageListMviVm::class.java.name
        val api = MainApplication.getApiRequestImplInstance()
    }

    private val _uiState = MutableStateFlow(MessageListState())
    val uiState: StateFlow<MessageListState> = _uiState.asStateFlow()

    private val _effect = Channel<MessageListEffect>(Channel.BUFFERED)
    val effect: Flow<MessageListEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: MessageListIntent) {
        when (intent) {
            MessageListIntent.Initialize -> initialize()
            is MessageListIntent.SelectMessage -> onMessageItemClick(intent.position)
            is MessageListIntent.EditAgent -> onEditAgent(intent.position)
            MessageListIntent.CreateAgent -> sendEffect(MessageListEffect.OpenCreateAgent)
            MessageListIntent.Refresh -> refreshMessages()
            is MessageListIntent.AgentCreated -> {
                if (intent.created) {
                    refreshMessages()
                }
            }
            MessageListIntent.StartChat -> sendEffect(MessageListEffect.RequestAudioPermission)
        }
    }

    private fun initialize() {
        loadCachedMessages()
        refreshMessages(showLoading = _uiState.value.messages.isEmpty())
    }

    private fun loadCachedMessages() {
        val cachedMessages = MainApplication.getMessageListManager().messageContactItemAos
        _uiState.update {
            val hasMessage = cachedMessages.isNotEmpty()
            it.copy(
                messages = cachedMessages.toList(),
                messageCount = cachedMessages.size,
                hasMessage = hasMessage,
                uiMode = deriveUiMode(it.hasAgent, hasMessage)
            )
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
        if (userId.isBlank()) {
            handleError("用户未登录")
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }

        supervisorScope {
            launch(Dispatchers.IO) { fetchAndHandleAgentList(userId) }  // 如果需要IO线程，在这里指定
            launch(Dispatchers.IO) { fetchAndHandleChatList(userId) }   // 如果需要IO线程，在这里指定
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                uiMode = deriveUiMode(it.hasAgent, it.hasMessage)
            )
        }
    }

    private suspend fun fetchAndHandleAgentList(userId: String) {
        runCatching { api.getAgentList(userId) }
            .onSuccess { response ->
                val hasAgent = (response.agentAos?.size ?: 0) > 0
                _uiState.update {
                    it.copy(
                        agentCount = response.agentAos?.size ?: 0,
                        hasAgent = hasAgent,
                        error = null
                    )
                }
            }
            .onFailure { exception ->
                Log.e(TAG, "获取Agent列表失败", exception)
                when (exception) {
                    is NetworkBusinessException -> {
                        exception.message?.let {
                            handleError(it)
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        error = "获取Agent列表失败: ${exception.message}",
                        hasAgent = false,
                        agentCount = 0
                    )
                }
            }
    }

    private suspend fun fetchAndHandleChatList(userId: String) {
        runCatching { api.getLastAgentChatList(userId) }
            .onSuccess { response ->
                val messages = response.agentChatAos.orEmpty()
                val messageContactItemAos = MessageConvertor.agentChatAos2MessageContactItemAos(messages)
                MainApplication.getMessageListManager().setAgentChatAos(response)

                _uiState.update {
                    it.copy(
                        messages = messageContactItemAos,
                        messageCount = messages.size,
                        hasMessage = messages.isNotEmpty(),
                        error = null
                    )
                }
            }
            .onFailure { exception ->
                Log.e(TAG, "获取Chat列表失败", exception)
                when (exception) {
                    is NetworkBusinessException -> {
                        exception.message?.let {
                            handleError(it)
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        error = "获取消息列表失败: ${exception.message}",
                        messages = emptyList(),
                        messageCount = 0,
                        hasMessage = false
                    )
                }
            }
    }

    private fun handleError(error: String) {
        Log.e(TAG, "handleError: $error")
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
    data class AgentCreated(val created: Boolean) : MessageListIntent()
    data object StartChat : MessageListIntent()
}

data class MessageListState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val messages: List<MessageContactItemAo> = emptyList(),
    val messageCount: Int = 0,
    val agentCount: Int = 0,
    val hasAgent: Boolean = false,
    val hasMessage: Boolean = false,
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
    data object NavigateToChatActivity : MessageListEffect()
    data class ShowToast(val message: String) : MessageListEffect()
    data object RefreshNetworkData : MessageListEffect()
    data object RequestAudioPermission : MessageListEffect()
}
