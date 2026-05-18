package com.vectordemo.viewModel.chat

import com.vectordemo.di.AppContainer
import com.vectordemo.domain.platform.currentTimeMillis
import com.vectordemo.service.ai.ChatService
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class RealtimeChatStatus {
    NOT_INITIALIZED,
    INITIALIZING,
    CONNECTED,
    SENDING,
    RECEIVING,
    ERROR,
}

data class ChatListMessage(
    val id: String,
    val role: String,
    val text: String,
    val createdAtMs: Long,
)

data class ChatListUiState(
    val status: RealtimeChatStatus = RealtimeChatStatus.NOT_INITIALIZED,
    val messages: List<ChatListMessage> = emptyList(),
    val errorMessage: String = "",
)

class ChatListVm(
    private val aliChatServiceProvider: () -> ChatService = { AppContainer.aliChatService },
) : BaseVm() {
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val history = mutableListOf<Map<String, String>>()
    private var systemPrompt: String = "You are a helpful assistant."

    fun initialize() {
        if (_uiState.value.status != RealtimeChatStatus.NOT_INITIALIZED) return
        vmScope.launch {
            _uiState.update { it.copy(status = RealtimeChatStatus.INITIALIZING) }
            _uiState.update { it.copy(status = RealtimeChatStatus.CONNECTED, errorMessage = "") }
        }
    }

    fun sendMessage(input: String) {
        val message = input.trim()
        if (message.isBlank() || _uiState.value.status != RealtimeChatStatus.CONNECTED) return
        if (!AppContainer.isInitialized) {
            _uiState.update {
                it.copy(
                    status = RealtimeChatStatus.ERROR,
                    errorMessage = "配置尚未加载完成，请稍后重试",
                )
            }
            return
        }

        vmScope.launch {
            val userMsg = ChatListMessage(newId(), "user", message, now())
            _uiState.update { st ->
                st.copy(status = RealtimeChatStatus.SENDING, messages = st.messages + userMsg, errorMessage = "")
            }
            history.add(mapOf("role" to "user", "content" to message))
            trimHistory()

            val assistantId = newId()
            _uiState.update { st ->
                st.copy(
                    status = RealtimeChatStatus.RECEIVING,
                    messages = st.messages + ChatListMessage(assistantId, "assistant", "", now()),
                )
            }

            runCatching {
                aliChatServiceProvider().sendChat(
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
                    },
                )
            }.onFailure {
                _uiState.update { st ->
                    st.copy(status = RealtimeChatStatus.ERROR, errorMessage = "消息发送失败: ${it.message}")
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

    private fun newId(): String = "${now()}-${Random.nextInt(1000, 9999)}"
    private fun now(): Long = currentTimeMillis()
}
