package com.vectordemo.viewModel.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vectordemo.config.ModuleKeyConfigStore
import com.vectordemo.service.ai.AliChatService
import com.vectordemo.service.ai.ChatService
import com.vectordemo.service.ai.PromptAssetLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class RealtimeChatStatus {
    NOT_INITIALIZED,
    INITIALIZING,
    CONNECTED,
    SENDING,
    RECEIVING,
    ERROR
}

data class ChatListMessage(
    val id: String,
    val role: String,
    val text: String,
    val createdAtMs: Long
)

data class ChatListUiState(
    val status: RealtimeChatStatus = RealtimeChatStatus.NOT_INITIALIZED,
    val messages: List<ChatListMessage> = emptyList(),
    val errorMessage: String = ""
)

class ChatListVm(
    private val context: Context,
    private val chatService: ChatService = AliChatService { ModuleKeyConfigStore.load(context).aliLlm }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val history = mutableListOf<Map<String, String>>()
    private var systemPrompt: String = ""

    fun initialize() {
        if (_uiState.value.status != RealtimeChatStatus.NOT_INITIALIZED) return
        viewModelScope.launch {
            _uiState.update { it.copy(status = RealtimeChatStatus.INITIALIZING) }
            runCatching {
                systemPrompt = PromptAssetLoader.loadSystemPrompt(context)
                _uiState.update { it.copy(status = RealtimeChatStatus.CONNECTED, errorMessage = "") }
            }.onFailure {
                _uiState.update { st ->
                    st.copy(
                        status = RealtimeChatStatus.ERROR,
                        errorMessage = "读取系统提示词失败: ${it.message}"
                    )
                }
            }
        }
    }

    fun sendMessage(input: String) {
        val message = input.trim()
        if (message.isBlank() || _uiState.value.status != RealtimeChatStatus.CONNECTED) {
            return
        }

        viewModelScope.launch {
            val userMsg = ChatListMessage(UUID.randomUUID().toString(), "user", message, System.currentTimeMillis())
            _uiState.update { st ->
                st.copy(
                    status = RealtimeChatStatus.SENDING,
                    messages = st.messages + userMsg,
                    errorMessage = ""
                )
            }
            history.add(mapOf("role" to "user", "content" to message))
            trimHistory()

            val assistantId = UUID.randomUUID().toString()
            _uiState.update { st ->
                st.copy(
                    status = RealtimeChatStatus.RECEIVING,
                    messages = st.messages + ChatListMessage(assistantId, "assistant", "", System.currentTimeMillis())
                )
            }

            runCatching {
                chatService.sendChat(
                    systemPrompt = systemPrompt,
                    history = history.toList(),
                    userMessage = message,
                    onDelta = { delta ->
                        if (delta.isBlank()) return@sendChat
                        _uiState.update { st ->
                            st.copy(messages = st.messages.map {
                                if (it.id == assistantId) it.copy(text = it.text + delta) else it
                            })
                        }
                    },
                    onDone = {
                        val finalText = _uiState.value.messages.firstOrNull { it.id == assistantId }?.text.orEmpty()
                        history.add(mapOf("role" to "assistant", "content" to finalText))
                        trimHistory()
                    }
                )
            }.onFailure {
                _uiState.update { st ->
                    st.copy(
                        status = RealtimeChatStatus.ERROR,
                        errorMessage = "消息发送失败: ${it.message}"
                    )
                }
            }

            if (_uiState.value.status != RealtimeChatStatus.ERROR) {
                _uiState.update { it.copy(status = RealtimeChatStatus.CONNECTED) }
            }
        }
    }

    private fun trimHistory() {
        var total = history.sumOf { it["content"]?.length ?: 0 }
        while (history.size > 2 && total > 12000) {
            val removed = history.removeAt(0)
            total -= removed["content"]?.length ?: 0
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatListVm(context.applicationContext) as T
            }
        }
    }
}

