package com.magicvector.manager.realtime

import com.magicvector.domain.constant.VadChatState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 实时聊天事件流
 */
class RealtimeChatEventFlow(
    private val coroutineScope: CoroutineScope
) {
    private val _vadStateEvents = MutableSharedFlow<VadChatState>()
    val vadStateEvents: SharedFlow<VadChatState> = _vadStateEvents.asSharedFlow()

    private val _agentTextEvents = MutableSharedFlow<String>()
    val agentTextEvents: SharedFlow<String> = _agentTextEvents.asSharedFlow()

    private val _realtimeState = MutableStateFlow<RealtimeChatState>(RealtimeChatState.NotInitialized)
    val realtimeState: StateFlow<RealtimeChatState> = _realtimeState.asStateFlow()

    private val _uiState = MutableStateFlow(RealtimeChatUiState())
    val uiState: StateFlow<RealtimeChatUiState> = _uiState.asStateFlow()

    /**
     * 发送 VAD 状态事件
     */
    fun emitVadState(state: VadChatState) {
        coroutineScope.launch {
            _vadStateEvents.emit(state)
        }
    }

    /**
     * 发送 Agent 文本事件
     */
    fun emitAgentText(text: String) {
        coroutineScope.launch {
            _agentTextEvents.emit(text)
        }
    }

    /**
     * 更新实时连接状态
     */
    fun updateRealtimeState(state: RealtimeChatState) {
        _realtimeState.value = state
    }

    /**
     * 更新 UI 状态
     */
    fun updateUiState(update: (RealtimeChatUiState) -> RealtimeChatUiState) {
        _uiState.value = update(_uiState.value)
    }
}