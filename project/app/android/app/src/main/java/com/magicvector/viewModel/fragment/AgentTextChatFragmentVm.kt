package com.magicvector.viewModel.fragment

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.domain.constant.chat.RoleTypeEnum
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.domain.vo.message.ChatMessageVO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgentTextChatFragmentVm : ViewModel() {

    /** MVI: UI 状态。 */
    private val _uiState = MutableStateFlow(AgentTextChatFragmentState())
    val uiState: StateFlow<AgentTextChatFragmentState> = _uiState.asStateFlow()

    /** MVI: 页面副作用。 */
    private val _effect = Channel<AgentTextChatFragmentEffect>(Channel.BUFFERED)
    val effect: Flow<AgentTextChatFragmentEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: AgentTextChatFragmentIntent) {
        when (intent) {
            is AgentTextChatFragmentIntent.SyncSendEnable -> {
                _uiState.update { it.copy(isEnableSend = intent.enable) }
            }
            is AgentTextChatFragmentIntent.SyncMessages -> {
                _uiState.update { it.copy(messages = intent.messages) }
            }
            is AgentTextChatFragmentIntent.UserSendText -> {
                val message = intent.message.trim()
                if (message.isEmpty()) return
                val now = nowText()
                val sent = ChatMessageModel(
                    chatMessageVo = ChatMessageVO(
                        briefMessageVo = ChatBriefMessageVO(
                            content = message,
                            chatTime = now,
                            role = RoleTypeEnum.USER.value
                        ),
                        imgUrl = "",
                        messageType = com.magicvector.domain.constant.chat.MessageTypeEnum.TEXT.value
                    ),
                    agentId = 0L,
                    userId = 0L,
                    messageId = System.currentTimeMillis(),
                    timestamp = System.currentTimeMillis()
                )
                sendEffect(AgentTextChatFragmentEffect.AppendMessage(sent))
                sendEffect(AgentTextChatFragmentEffect.ForwardSendText(message))
            }
            is AgentTextChatFragmentIntent.ReceiveAgentText -> {
                if (intent.text.isBlank()) return
                val receive = ChatMessageModel(
                    chatMessageVo = ChatMessageVO(
                        briefMessageVo = ChatBriefMessageVO(
                            content = intent.text,
                            chatTime = nowText(),
                            role = RoleTypeEnum.AGENT.value
                        ),
                        imgUrl = "",
                        messageType = com.magicvector.domain.constant.chat.MessageTypeEnum.TEXT.value
                    ),
                    agentId = 0L,
                    userId = 0L,
                    messageId = System.currentTimeMillis(),
                    timestamp = System.currentTimeMillis()
                )
                sendEffect(AgentTextChatFragmentEffect.AppendMessage(receive))
            }
            AgentTextChatFragmentIntent.SwitchToEmojiPage -> {
                sendEffect(AgentTextChatFragmentEffect.ForwardSwitchToEmojiPage)
            }
            is AgentTextChatFragmentIntent.OnAudioTouch -> {
                if (intent.isStart) {
                    sendEffect(AgentTextChatFragmentEffect.ForwardStartSendVoice)
                } else {
                    sendEffect(AgentTextChatFragmentEffect.ForwardStopSendVoice)
                }
            }
        }
    }

    private fun nowText(): String {
        return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
    }

    private fun sendEffect(effect: AgentTextChatFragmentEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

@Stable
data class AgentTextChatFragmentState(
    /** 输入与按钮可用态。 */
    val isEnableSend: Boolean = false,
    /** 当前聊天记录快照。 */
    val messages: List<ChatMessageModel> = emptyList()
)

sealed class AgentTextChatFragmentIntent {
    /** 同步发送可用状态。 */
    data class SyncSendEnable(val enable: Boolean) : AgentTextChatFragmentIntent()

    /** 用户发送文本。 */
    data class UserSendText(val message: String) : AgentTextChatFragmentIntent()

    /** 同步历史记录和当前会话快照。 */
    data class SyncMessages(val messages: List<ChatMessageModel>) : AgentTextChatFragmentIntent()

    /** 收到 Agent 文本。 */
    data class ReceiveAgentText(val text: String) : AgentTextChatFragmentIntent()

    /** 切换到 Emoji 页。 */
    data object SwitchToEmojiPage : AgentTextChatFragmentIntent()

    /** 录音按压事件。 */
    data class OnAudioTouch(val isStart: Boolean) : AgentTextChatFragmentIntent()
}

sealed class AgentTextChatFragmentEffect {
    data class AppendMessage(val message: ChatMessageModel) : AgentTextChatFragmentEffect()
    data class ForwardSendText(val text: String) : AgentTextChatFragmentEffect()
    data object ForwardSwitchToEmojiPage : AgentTextChatFragmentEffect()
    data object ForwardStartSendVoice : AgentTextChatFragmentEffect()
    data object ForwardStopSendVoice : AgentTextChatFragmentEffect()
}