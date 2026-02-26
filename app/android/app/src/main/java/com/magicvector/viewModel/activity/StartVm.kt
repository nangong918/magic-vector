package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.constant.BaseConstant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class StartVm : ViewModel() {
    companion object {
        val TAG: String = StartVm::class.java.name
    }


    //--------------------State--------------------

    private val _uiState = MutableStateFlow(StartState())
    val uiState: StateFlow<StartState> = _uiState.asStateFlow()
    private val _effect = Channel<StartEffect>(Channel.BUFFERED)
    val effect: Flow<StartEffect> = _effect.receiveAsFlow()
    private fun sendEffect(effect: StartEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }


    //--------------------Intent--------------------

    fun processIntent(intent: StartIntent) {
        when (intent) {
            is StartIntent.Initialize -> {
                initialize()
            }
        }
    }

    //--------------------Logic--------------------

    private fun initialize() {
        _uiState.update { it.copy(isLoading = true) }

        // 延时启动
        startCountdownWithCoroutine()
    }

    // 使用协程 delay
    private fun startCountdownWithCoroutine() {
        viewModelScope.launch {
            // 延迟指定时间
            delay(BaseConstant.Constant.START_DELAY_TIME)

            // 延迟结束后发送导航 Effect
            _uiState.update { it.copy(isLoading = false) }
            sendEffect(StartEffect.NavigateToMain)
        }
    }
}

sealed class StartIntent {
    object Initialize : StartIntent()
}

data class StartState(
    val isLoading: Boolean = false,
    val isCountingDown: Boolean = true
)

sealed class StartEffect {
    object NavigateToMain : StartEffect()
}



