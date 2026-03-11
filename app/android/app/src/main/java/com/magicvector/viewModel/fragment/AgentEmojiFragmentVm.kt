package com.magicvector.viewModel.fragment

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.viewModel.activity.AgentVoiceOrbPhase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentEmojiFragmentVm : ViewModel() {

    /** MVI: 页面渲染状态。 */
    private val _uiState = MutableStateFlow(AgentEmojiFragmentState())
    val uiState: StateFlow<AgentEmojiFragmentState> = _uiState.asStateFlow()

    /** MVI: 页面副作用。 */
    private val _effect = Channel<AgentEmojiFragmentEffect>(Channel.BUFFERED)
    val effect: Flow<AgentEmojiFragmentEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: AgentEmojiFragmentIntent) {
        when (intent) {
            is AgentEmojiFragmentIntent.SyncPhase -> {
                _uiState.update {
                    it.copy(
                        phase = intent.phase,
                        isOrbExpanded = intent.expanded,
                        statusText = intent.statusText
                    )
                }
            }
            AgentEmojiFragmentIntent.ToggleMic -> {
                sendEffect(AgentEmojiFragmentEffect.ForwardToggleMic)
            }
            AgentEmojiFragmentIntent.RequestWakeUp -> {
                sendEffect(AgentEmojiFragmentEffect.ForwardRequestWakeUp)
            }
            AgentEmojiFragmentIntent.EndVoiceMode -> {
                sendEffect(AgentEmojiFragmentEffect.ForwardEndVoiceMode)
            }
        }
    }

    private fun sendEffect(effect: AgentEmojiFragmentEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

@Stable
data class AgentEmojiFragmentState(
    /** 语音状态球阶段。 */
    val phase: AgentVoiceOrbPhase = AgentVoiceOrbPhase.DISCONNECTED,
    /** 状态球是否放大。 */
    val isOrbExpanded: Boolean = false,
    /** 状态文案。 */
    val statusText: String = "未连接"
)

sealed class AgentEmojiFragmentIntent {
    /** 同步 Activity 层语音阶段。 */
    data class SyncPhase(
        val phase: AgentVoiceOrbPhase,
        val expanded: Boolean,
        val statusText: String
    ) : AgentEmojiFragmentIntent()

    /** 切换麦克风。 */
    data object ToggleMic : AgentEmojiFragmentIntent()

    /** 请求唤醒。 */
    data object RequestWakeUp : AgentEmojiFragmentIntent()

    /** 结束语音模式。 */
    data object EndVoiceMode : AgentEmojiFragmentIntent()
}

sealed class AgentEmojiFragmentEffect {
    data object ForwardToggleMic : AgentEmojiFragmentEffect()
    data object ForwardRequestWakeUp : AgentEmojiFragmentEffect()
    data object ForwardEndVoiceMode : AgentEmojiFragmentEffect()
}
