package com.magicvector.viewModel.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class MineVm() : ViewModel(){

    companion object {
        val TAG: String = MineVm::class.java.name
    }

    // UI State
    private val _uiState = MutableStateFlow(MineState())
    val uiState: StateFlow<MineState> = _uiState.asStateFlow()

    // Effect
    private val _effect = Channel<MineEffect>()
    val effect = _effect.receiveAsFlow()

    // 处理 Intent
    fun processIntent(intent: MineIntent) {
        when (intent) {
            is MineIntent.Initialize -> {
                initialize()
            }
            is MineIntent.TestButtonClick -> {
                onTestButtonClick()
            }
        }
    }

    private fun initialize() {
        // 初始化逻辑，比如加载用户信息
        viewModelScope.launch {
            // 模拟加载
            _uiState.update {
                it.copy(
                    isLoading = true
                )
            }

            // 这里可以调用 repository 加载数据
            // val userInfo = userRepository.getUserInfo()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    userName = "测试用户"
                )
            }
        }
    }


    private fun onTestButtonClick() {
        viewModelScope.launch {
            // 可以在这里执行一些操作，比如埋点
            // analytics.logEvent("test_button_clicked")

            // 发送 Effect 触发导航
            _effect.send(MineEffect.NavigateToTest)
        }
    }

    // 辅助函数：更新 State
    private fun MutableStateFlow<MineState>.update(block: (MineState) -> MineState) {
        value = block(value)
    }
}

sealed class MineIntent {
    // 初始化意图 - 界面启动时调用
    data object Initialize : MineIntent()
    // 测试按钮点击意图 - 用户点击测试按钮
    data object TestButtonClick : MineIntent()
}

data class MineState(
    val isLoading: Boolean = false,
    // 可以添加其他状态，比如用户信息等
    val userName: String = "",
    val userAvatar: String? = null
)

sealed class MineEffect {
    // 测试按钮点击效果 - 跳转到测试界面
    data object NavigateToTest : MineEffect()
    // 显示 Toast 效果 - 显示 Toast
    data class ShowToast(val message: String) : MineEffect()
}