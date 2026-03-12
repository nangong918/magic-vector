package com.magicvector.viewModel.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.activity.test.ComposeTestActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MineVm : ViewModel() {

    /** MVI: UI 渲染状态。 */
    private val _uiState = MutableStateFlow(MineState())
    val uiState: StateFlow<MineState> = _uiState.asStateFlow()

    /** MVI: 一次性副作用。 */
    private val _effect = Channel<MineEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun processIntent(intent: MineIntent) {
        when (intent) {
            MineIntent.Initialize -> initialize()
            MineIntent.TestButtonClick -> sendEffect(MineEffect.NavigateToActivity(ComposeTestActivity::class.java.name))
            MineIntent.PickLocalVideoClick -> sendEffect(MineEffect.OpenLocalVideoPicker)
            is MineIntent.OnLocalVideoSelected -> _uiState.update {
                it.copy(selectedVideoUri = intent.uriString, isVideoPlaying = intent.uriString != null)
            }
            MineIntent.ToggleVideoPlay -> _uiState.update { it.copy(isVideoPlaying = !it.isVideoPlaying) }
        }
    }

    private fun initialize() {
        _uiState.update {
            it.copy(
                userName = "本地用户",
                settingTodo = "TODO: 修改密码 / 登出",
                cloudReplayTodo = "TODO: 云上录播记录播放",
                uploadTodo = "TODO: 本地视频上传云端"
            )
        }
    }

    private fun sendEffect(effect: MineEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private fun MutableStateFlow<MineState>.update(block: (MineState) -> MineState) {
        value = block(value)
    }
}

sealed class MineIntent {
    data object Initialize : MineIntent()
    data object TestButtonClick : MineIntent()
    data object PickLocalVideoClick : MineIntent()
    data class OnLocalVideoSelected(val uriString: String?) : MineIntent()
    data object ToggleVideoPlay : MineIntent()
}

data class MineState(
    /** 页面昵称。 */
    val userName: String = "",
    /** 本地视频 Uri。 */
    val selectedVideoUri: String? = null,
    /** 当前是否播放。 */
    val isVideoPlaying: Boolean = false,
    /** 设置模块 TODO。 */
    val settingTodo: String = "",
    /** 云录播 TODO。 */
    val cloudReplayTodo: String = "",
    /** 上传云端 TODO。 */
    val uploadTodo: String = ""
)

sealed class MineEffect {
    data object OpenLocalVideoPicker : MineEffect()
    data class NavigateToActivity(val activityClassName: String) : MineEffect()
}