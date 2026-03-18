package com.magicvector.viewModel.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.activity.test.ComposeTestActivity
import com.magicvector.MainApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Mine 首页入口 ViewModel（MVI）：
 * - 仅负责「设置 / 视频 / 测试」三个入口导航，不承载子页面业务逻辑。
 */
class MineVm : ViewModel() {

    /** MVI: Mine 首页 UI 状态。 */
    private val _uiState = MutableStateFlow(MineState())
    val uiState: StateFlow<MineState> = _uiState.asStateFlow()
    /** MVI: Mine 首页业务数据状态。 */
    private val _dataState = MutableStateFlow(MineDataState())
    val dataState: StateFlow<MineDataState> = _dataState.asStateFlow()

    /** MVI: 一次性副作用（页面跳转）。 */
    private val _effect = Channel<MineEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun processIntent(intent: MineIntent) {
        when (intent) {
            MineIntent.Initialize -> initialize()
            MineIntent.NotifyHomeRefresh -> {
                _dataState.value = _dataState.value.copy(homeRefreshToken = System.currentTimeMillis())
            }
            MineIntent.OpenSettingPage -> sendEffect(MineEffect.NavigateToSetting)
            MineIntent.OpenVideoPage -> sendEffect(MineEffect.NavigateToVideo)
            MineIntent.TestButtonClick -> sendEffect(MineEffect.NavigateToActivity(ComposeTestActivity::class.java.name))
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            val user = MainApplication.getUserManager().getCurrentUser()
            _uiState.update {
                it.copy(userName = if (user?.account.isNullOrBlank()) "本地用户" else user?.account.orEmpty())
            }
        }
    }

    private fun sendEffect(effect: MineEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun MutableStateFlow<MineState>.update(block: (MineState) -> MineState) {
        value = block(value)
    }
}

/** Mine 首页 Intent。 */
sealed class MineIntent {
    data object Initialize : MineIntent()
    data object NotifyHomeRefresh : MineIntent()
    data object OpenSettingPage : MineIntent()
    data object OpenVideoPage : MineIntent()
    data object TestButtonClick : MineIntent()
}

/** Mine 首页状态。 */
data class MineState(
    /** 展示用用户名。 */
    val userName: String = ""
)

data class MineDataState(
    val homeRefreshToken: Long = 0L
)

/** Mine 首页副作用。 */
sealed class MineEffect {
    data object NavigateToSetting : MineEffect()
    data object NavigateToVideo : MineEffect()
    data class NavigateToActivity(val activityClassName: String) : MineEffect()
    data class ShowToast(val message: String) : MineEffect()
}
