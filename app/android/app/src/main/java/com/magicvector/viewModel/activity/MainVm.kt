package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.manager.RealtimeChatController
import com.view.appview.MainSelectItemEnum
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainVm : ViewModel() {

    companion object {
        val TAG: String = MainVm::class.java.name
    }

    // 保留对外控制器引用，避免影响其他页面后续接入。
    var realtimeChatController: RealtimeChatController? = null

    // StateFlow (UI State) 存储 UI 状态
    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    // Channel/Flow (Effect) 发送一次性事件 （类似EventBus、广播）
    private val _effect = Channel<MainEffect>(Channel.BUFFERED)
    val effect: Flow<MainEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.Initialize -> {
                _uiState.update {
                    it.copy(
                        currentSelected = intent.selected
                    )
                }
            }

            is MainIntent.SelectTab -> {
                _uiState.update {
                    it.copy(
                        currentSelected = intent.tab
                    )
                }
            }

            MainIntent.OpenCreateAgent -> {
                sendEffect(MainEffect.LaunchCreateAgent)
            }

            is MainIntent.ChatServiceBound -> {
                realtimeChatController = intent.handler
                _uiState.update { it.copy(isChatServiceBound = true) }
            }

            MainIntent.ChatServiceUnbound -> {
                realtimeChatController = null
                _uiState.update { it.copy(isChatServiceBound = false) }
            }
        }
    }

    private fun sendEffect(effect: MainEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}


sealed class MainIntent {
    // 初始化意图 - 页面启动时调用
    data class Initialize(val selected: MainSelectItemEnum) : MainIntent()
    // 作用：告诉 ViewModel 页面启动了，并且需要选中某个 Tab
    // 触发时机：onCreate 时调用

    // 切换 Tab 意图 - 用户点击底部导航栏
    data class SelectTab(val tab: MainSelectItemEnum) : MainIntent()
    // 作用：用户想要切换到某个页面（首页/应用/我的）
    // 触发时机：底部导航栏点击时

    // Service 绑定成功意图 - 系统回调
    data class ChatServiceBound(val handler: RealtimeChatController) : MainIntent()
    // 作用：ChatService 已经连接成功，把通信处理器传给 ViewModel
    // 触发时机：ServiceConnection.onServiceConnected

    // Service 解绑意图 - 系统回调
    data object ChatServiceUnbound : MainIntent()
    // 作用：ChatService 断开连接了，需要更新 UI 状态
    // 触发时机：ServiceConnection.onServiceDisconnected 或 onDestroy

    // 打开创建 Agent 意图 - 用户点击按钮
    data object OpenCreateAgent : MainIntent()
    // 作用：用户想要跳转到创建 Agent 页面
    // 触发时机：点击"创建Agent"按钮
}

data class MainState(
    val currentSelected: MainSelectItemEnum = MainSelectItemEnum.HOME,
    val isChatServiceBound: Boolean = false
)

sealed class MainEffect {
    // 跳转创建 Agent 页面
    data object LaunchCreateAgent : MainEffect()
}