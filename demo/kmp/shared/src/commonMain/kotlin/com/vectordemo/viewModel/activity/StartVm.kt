package com.vectordemo.viewModel.activity

import com.vectordemo.di.AppContainer
import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartVm : BaseVm() {
    private val _uiState = MutableStateFlow(StartState())
    val uiState: StateFlow<StartState> = _uiState.asStateFlow()

    private val _effect = Channel<StartEffect>(Channel.BUFFERED)
    val effect: Flow<StartEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: StartIntent) {
        if (intent is StartIntent.Initialize) initialize()
    }

    private fun initialize() {
        _uiState.update { it.copy(isLoading = true) }
        vmScope.launch {
            val localUser = AppContainer.userManager.getCurrentUser()
            val target = if (localUser == null || localUser.accessToken.isBlank()) {
                StartEffect.NavigateToLogin
            } else {
                try {
                    val verify = AppContainer.userRemote.verifyAccessToken(
                        userId = localUser.userId,
                        accessToken = localUser.accessToken,
                    )
                    if (verify) {
                        AppContainer.updateUserId(localUser.userId)
                        AppContainer.updateToken(localUser.accessToken)
                        StartEffect.NavigateToMain
                    } else {
                        AppContainer.userManager.clearCurrentUser()
                        AppContainer.clearUserId()
                        AppContainer.updateToken(null)
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

sealed class StartIntent {
    data object Initialize : StartIntent()
}

data class StartState(val isLoading: Boolean = false)

sealed class StartEffect {
    data object NavigateToMain : StartEffect()
    data object NavigateToLogin : StartEffect()
    data class ShowToast(val message: String) : StartEffect()
}
