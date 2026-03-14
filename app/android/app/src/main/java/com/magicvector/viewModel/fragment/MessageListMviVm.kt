package com.magicvector.viewModel.fragment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.constant.BaseConstant
import com.data.domain.dto.response.AgentLastChatListResponse
import com.data.domain.dto.response.AgentListResponse
import com.magicvector.MainApplication
import com.magicvector.convertor.MessageConvertor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MessageListMviVm : ViewModel() {

    companion object {
        val TAG: String = MessageListMviVm::class.java.name
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

    private suspend fun syncAgentAndMessageState() {
        try {
            val userId = resolveUserId()
            if (userId.isBlank()) {
                handleError("用户未登录")
                return
            }
            val (agentListResp, chatListResp) = withContext(Dispatchers.IO) {
                coroutineScope {
                    val agentListDeferred = async { requestAgentList(userId) }
                    val chatListDeferred = async { requestLastAgentChatList(userId) }
                    agentListDeferred.await() to chatListDeferred.await()
                }
            }
            handleAgentAndChatResponse(agentListResp, chatListResp)
        } catch (e: Exception) {
            handleError(e.message ?: "网络请求失败")
        } finally {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
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

    private suspend fun requestAgentList(userId: String): BaseResponse<AgentListResponse> {
        return suspendCoroutine { continuation ->
            MainApplication.getApiRequestImplInstance().getAgentList(
                userId,
                object : OnSuccessCallback<BaseResponse<AgentListResponse>> {
                    override fun onResponse(response: BaseResponse<AgentListResponse>?) {
                        if (response != null) {
                            continuation.resume(response)
                        } else {
                            continuation.resumeWithException(Exception("获取Agent列表返回空响应"))
                        }
                    }
                },
                object : OnThrowableCallback {
                    override fun callback(throwable: Throwable?) {
                        continuation.resumeWithException(throwable ?: Exception("获取Agent列表失败"))
                    }
                }
            )
        }
    }

    private suspend fun requestLastAgentChatList(userId: String): BaseResponse<AgentLastChatListResponse> {
        return suspendCoroutine { continuation ->
            MainApplication.getApiRequestImplInstance().getLastAgentChatList(
                userId,
                object : OnSuccessCallback<BaseResponse<AgentLastChatListResponse>> {
                    override fun onResponse(response: BaseResponse<AgentLastChatListResponse>?) {
                        if (response != null) {
                            continuation.resume(response)
                        } else {
                            continuation.resumeWithException(Exception("获取最近聊天返回空响应"))
                        }
                    }
                },
                object : OnThrowableCallback {
                    override fun callback(throwable: Throwable?) {
                        continuation.resumeWithException(throwable ?: Exception("获取最近聊天失败"))
                    }
                }
            )
        }
    }

    private fun handleAgentAndChatResponse(
        agentListResponse: BaseResponse<AgentListResponse>,
        chatListResponse: BaseResponse<AgentLastChatListResponse>
    ) {
        val agentListSuccess = agentListResponse.code == BaseConstant.NetworkCode.SUCCESS_CODE
        if (!agentListSuccess) {
            handleError(agentListResponse.message ?: "获取Agent列表失败")
            return
        }
        val chatListSuccess = chatListResponse.code == BaseConstant.NetworkCode.SUCCESS_CODE
        if (!chatListSuccess) {
            handleError(chatListResponse.message ?: "获取最近聊天失败")
            return
        }
        val hasAgent = (agentListResponse.data?.agentAos?.size ?: 0) > 0
        val chatData = chatListResponse.data
        val messages = chatData?.agentChatAos.orEmpty()
        val messageContactItemAos = MessageConvertor.agentChatAos2MessageContactItemAos(messages)
        if (chatData != null) {
            MainApplication.getMessageListManager().setAgentChatAos(chatData)
        } else {
            MainApplication.getMessageListManager().clear()
        }
        _uiState.update {
            val hasMessage = messages.isNotEmpty()
            it.copy(
                isFirstOpen = false,
                error = null,
                messages = messageContactItemAos,
                messageCount = messages.size,
                agentCount = agentListResponse.data?.agentAos?.size ?: 0,
                hasAgent = hasAgent,
                hasMessage = hasMessage,
                uiMode = deriveUiMode(hasAgent, hasMessage)
            )
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
