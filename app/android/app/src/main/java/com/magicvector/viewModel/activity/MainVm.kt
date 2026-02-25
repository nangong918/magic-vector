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

    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

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
    data class Initialize(val selected: MainSelectItemEnum) : MainIntent()
    data class SelectTab(val tab: MainSelectItemEnum) : MainIntent()
    data class ChatServiceBound(val handler: RealtimeChatController) : MainIntent()
    data object ChatServiceUnbound : MainIntent()
    data object OpenCreateAgent : MainIntent()
}

data class MainState(
    val currentSelected: MainSelectItemEnum = MainSelectItemEnum.HOME,
    val isChatServiceBound: Boolean = false
)

sealed class MainEffect {
    data object LaunchCreateAgent : MainEffect()
}