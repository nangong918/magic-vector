package com.magicvector.viewModel.fragment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.cache.HttpRequestManager
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.dto.response.AgentLastChatListResponse
import com.magicvector.MainApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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

    // 意图处理 （跟事件处理什么区别？）
    fun processIntent(intent: MessageListIntent) {
        when (intent) {
            is MessageListIntent.Initialize -> {
                initialize()
            }
            is MessageListIntent.SelectMessage -> {
                onMessageItemClick(intent.position)
            }
            MessageListIntent.CreateAgent -> {
                sendEffect(MessageListEffect.OpenCreateAgent)
            }
            MessageListIntent.Refresh -> {
                refreshMessages()
            }
            is MessageListIntent.AgentCreated -> {
                if (intent.created) {
                    refreshMessages()
                }
            }
            is MessageListIntent.StartChat -> {
                // 将权限请求的结果通过 Effect 返回
                sendEffect(MessageListEffect.RequestAudioPermission)
            }
        }
    }


    //---------------------------初始化---------------------------

    private fun initialize() {

        _uiState.update {
            it.copy(isLoading = true)
        }

        // 检查是否第一次打开 + 网络请求
        initNetworkRequest()

        _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadCachedMessages() {
        val cachedMessages = MainApplication.getMessageListManager().messageContactItemAos
        _uiState.update {
            it.copy(
                messages = cachedMessages.toList(),
                messageCount = cachedMessages.size
            )
        }
    }

    private fun refreshMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun onMessageItemClick(position: Int) {
        val messages = _uiState.value.messages
        if (messages.size > position) {
            val ao = messages[position]
            sendEffect(MessageListEffect.NavigateToChat(ao))
        }
    }

    //---------------------------工具方法---------------------------
    private fun sendEffect(effect: MessageListEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    //---------------------------Network---------------------------

    fun initNetworkRequest(){
        if (HttpRequestManager.getIsFirstOpen(TAG)){
            // 第一次打开，初始化
            Log.i(TAG, "initNetworkRequest: 第一次打开")
            sendEffect(MessageListEffect.ShowLoadingDialog)
            doGetLastAgentChatList()
        }
        else {
            Log.i(TAG, "initNetworkRequest: 不是第一次打开")
            sendEffect(MessageListEffect.DismissLoadingDialog)
            val messageContactItemAos = MainApplication.getMessageListManager().messageContactItemAos
            _uiState.update {
                it.copy(
                    messages = messageContactItemAos.toList(),
                    messageCount = messageContactItemAos.size
                )
            }
        }
    }

    // 网络请求直接在 ViewModel 中
    private fun doGetLastAgentChatList() {
        viewModelScope.launch {
            try {
                // 使用 suspend 函数或回调转协程
                val response = withContext(Dispatchers.IO) {
                    // 这里需要将回调转为 suspend 函数
                    suspendCoroutine { continuation ->
                        MainApplication.getApiRequestImplInstance().getLastAgentChatList(
                            MainApplication.getUserId(),
                            object : OnSuccessCallback<BaseResponse<AgentLastChatListResponse>> {
                                override fun onResponse(response: BaseResponse<AgentLastChatListResponse>?) {
                                    continuation.resume(response!!)
                                }
                            },
                            object : OnThrowableCallback {
                                override fun callback(throwable: Throwable?) {
                                    continuation.resumeWithException(throwable ?: Exception("Unknown error"))
                                }
                            }
                        )
                    }
                }

                // 处理响应
                handleResponse(response)

            } catch (e: Exception) {
                handleError(e.message ?: "网络请求失败")
            }
        }
    }

    private fun handleResponse(response: BaseResponse<AgentLastChatListResponse>) {
        if (response.data != null) {
            // 缓存
            MainApplication.getMessageListManager().setAgentChatAos(response.data!!)
            // 更新 State
            loadCachedMessages()
            _uiState.update {
                it.copy(
                    isFirstOpen = false,
                    isLoading = false
                )
            }
        } else {
            MainApplication.getMessageListManager().clear()
            // 更新 State
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    messageCount = 0,
                    isLoading = false
                )
            }
        }
    }

    private fun handleError(error: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = error
            )
        }
        sendEffect(MessageListEffect.ShowToast(error))
    }

}

sealed class MessageListIntent {
    data object Initialize : MessageListIntent()
    data class SelectMessage(val position: Int) : MessageListIntent()
    data object CreateAgent : MessageListIntent()
    data object Refresh : MessageListIntent()
    data class AgentCreated(val created: Boolean) : MessageListIntent()
    // 启动聊天
    data object StartChat : MessageListIntent()
}

data class MessageListState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val messages: List<MessageContactItemAo> = emptyList(),
    val messageCount: Int = 0,
    val error: String? = null,
    val isFirstOpen: Boolean = true,  // 是否第一次打开
    val needLoadNetworkData: Boolean = false  // 是否需要加载网络数据
)

sealed class MessageListEffect {
    data object OpenCreateAgent : MessageListEffect()
    data class NavigateToChat(val ao: MessageContactItemAo) : MessageListEffect()
    data object ShowLoadingDialog : MessageListEffect()
    data object DismissLoadingDialog : MessageListEffect()
    data object NavigateToChatActivity : MessageListEffect()
    data class ShowToast(val message: String) : MessageListEffect()
    data object LoadNetworkData : MessageListEffect()  // 加载网络数据
    data object RefreshNetworkData : MessageListEffect()  // 刷新网络数据
    data object RequestAudioPermission : MessageListEffect()  // 请求录音权限
}
