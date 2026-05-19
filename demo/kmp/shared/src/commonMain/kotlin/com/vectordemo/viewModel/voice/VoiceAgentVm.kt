package com.vectordemo.viewModel.voice

import com.vectordemo.domain.platform.PlatformFeatures
import androidx.lifecycle.viewModelScope
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VoiceAgentPhase {
    INITIALIZING, READY, WAKE_DETECTED_WAITING_SPEECH, USER_SPEAKING, USER_SPEECH_ENDED, AGENT_REPLYING, ERROR,
}

enum class VoiceAgentLlmProvider { ALI, XFYUN }

data class VoiceAgentUiState(
    val phase: VoiceAgentPhase = VoiceAgentPhase.INITIALIZING,
    val llmProvider: VoiceAgentLlmProvider = VoiceAgentLlmProvider.ALI,
    val logs: List<String> = emptyList(),
)

sealed class VoiceAgentEffect {
    data object FinishActivity : VoiceAgentEffect()
}

class VoiceAgentVm : BaseVm() {
    private val _uiState = MutableStateFlow(VoiceAgentUiState())
    val uiState: StateFlow<VoiceAgentUiState> = _uiState.asStateFlow()
    private val _effect = Channel<VoiceAgentEffect>(Channel.BUFFERED)
    val effect: Flow<VoiceAgentEffect> = _effect.receiveAsFlow()

    fun initialize() {
        if (!PlatformFeatures.supportsVoiceAgent) {
            _uiState.update {
                it.copy(
                    phase = VoiceAgentPhase.ERROR,
                    logs = listOf("iOS 暂不支持语音链路：该能力依赖 Android Service 与原生 SDK。"),
                )
            }
            sendEffect(VoiceAgentEffect.FinishActivity)
            return
        }
        _uiState.update { it.copy(phase = VoiceAgentPhase.READY, logs = listOf("语音能力初始化完成（Android）")) }
    }

    fun setLlmProvider(provider: VoiceAgentLlmProvider) {
        if (_uiState.value.llmProvider == provider) return
        _uiState.update { it.copy(llmProvider = provider) }
    }

    fun buildServiceStatusText(state: VoiceAgentUiState = _uiState.value): String {
        return if (PlatformFeatures.supportsVoiceAgent) {
            "语音链路: Android 可用（当前示例）"
        } else {
            "语音链路: iOS 暂不支持"
        }
    }

    private fun sendEffect(effect: VoiceAgentEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
