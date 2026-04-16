package com.vectordemo.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectordemo.MainApplication
import com.vectordemo.domain.constant.BaseConstant
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
    private val _uiState = MutableStateFlow(StartState())
    val uiState: StateFlow<StartState> = _uiState.asStateFlow()
    private val _effect = Channel<StartEffect>(Channel.BUFFERED)
    val effect: Flow<StartEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: StartIntent) {
        if (intent is StartIntent.Initialize) initialize()
    }

    private fun initialize() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val localUser = MainApplication.getUserManager().getCurrentUser()
            val target = if (localUser == null || localUser.accessToken.isBlank()) {
                StartEffect.NavigateToLogin
            } else {
                try {
                    val verify = MainApplication.getUserRemoteApiSource().verifyAccessToken(localUser.accessToken)
                    if (verify) {
                        MainApplication.updateUserId(localUser.userId)
                        StartEffect.NavigateToMain
                    } else {
                        MainApplication.getUserManager().clearCurrentUser()
                        MainApplication.clearUserId()
                        StartEffect.NavigateToLogin
                    }
                } catch (_: Throwable) {
                    StartEffect.NavigateToLogin
                }
            }
            delay(BaseConstant.Constant.START_DELAY_TIME)
            _uiState.update { it.copy(isLoading = false) }
            _effect.send(target)
        }
    }
}

sealed class StartIntent { data object Initialize : StartIntent() }
data class StartState(val isLoading: Boolean = false)
sealed class StartEffect {
    data object NavigateToMain : StartEffect()
    data object NavigateToLogin : StartEffect()
    data class ShowToast(val message: String) : StartEffect()
}
