package com.vectordemo.viewModel.activity

import androidx.lifecycle.viewModelScope
import com.vectordemo.dataSource.remote.UserRemoteApiSource
import com.vectordemo.di.AppContainer
import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.manager.user.UserManager
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

class StartVm(
    private val userManager: UserManager,
    private val userRemote: UserRemoteApiSource,
) : BaseVm() {
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
            val localUser = userManager.getCurrentUser()
            val target = if (localUser == null || localUser.accessToken.isBlank()) {
                StartEffect.NavigateToLogin
            } else {
                try {
                    val verify = userRemote.verifyAccessToken(
                        userId = localUser.userId,
                        accessToken = localUser.accessToken,
                    )
                    if (verify) {
                        userManager.saveCurrentUser(localUser)
                        AppContainer.updateUserId(localUser.userId)
                        AppContainer.updateToken(localUser.accessToken)
                        StartEffect.NavigateToMain
                    } else {
                        userManager.clearCurrentUser()
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
