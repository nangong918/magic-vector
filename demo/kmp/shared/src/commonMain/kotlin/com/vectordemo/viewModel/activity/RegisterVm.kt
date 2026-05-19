package com.vectordemo.viewModel.activity

import androidx.lifecycle.viewModelScope
import com.vectordemo.dataSource.remote.UserRemoteApiSource
import com.vectordemo.di.AppContainer
import com.vectordemo.manager.user.UserManager
import com.vectordemo.repository.api.MultipartPartPayload
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterVm(
    private val userManager: UserManager,
    private val userRemote: UserRemoteApiSource,
) : BaseVm() {
    private val _uiState = MutableStateFlow(RegisterState())
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()
    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect: Flow<RegisterEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateAccount -> _uiState.update { it.copy(account = intent.account) }
            is RegisterIntent.UpdatePassword -> _uiState.update { it.copy(password = intent.password) }
            is RegisterIntent.UpdateConfirmPassword -> _uiState.update { it.copy(confirmPassword = intent.confirmPassword) }
            RegisterIntent.SelectAvatar -> sendEffect(RegisterEffect.RequestStoragePermission)
            is RegisterIntent.AvatarSelected -> _uiState.update { it.copy(avatar = intent.avatar) }
            RegisterIntent.SubmitRegister -> submitRegister()
            RegisterIntent.NavigateToLogin -> sendEffect(RegisterEffect.NavigateToLogin)
        }
    }

    private fun submitRegister() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(RegisterEffect.ShowToast("请检查输入"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val session = userRemote.register(
                    avatar = state.avatar,
                    account = state.account.trim(),
                    password = state.password,
                    name = state.account.trim(),
                )
                userManager.saveCurrentUser(session)
                AppContainer.updateUserId(session.userId)
                AppContainer.updateToken(session.accessToken)
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(RegisterEffect.NavigateToMain)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(RegisterEffect.ShowToast("注册失败"))
            }
        }
    }

    private fun sendEffect(effect: RegisterEffect) = viewModelScope.launch { _effect.send(effect) }
}

sealed class RegisterIntent {
    data class UpdateAccount(val account: String) : RegisterIntent()
    data class UpdatePassword(val password: String) : RegisterIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    data object SelectAvatar : RegisterIntent()
    data class AvatarSelected(val avatar: MultipartPartPayload?) : RegisterIntent()
    data object SubmitRegister : RegisterIntent()
    data object NavigateToLogin : RegisterIntent()
}

data class RegisterState(
    val account: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val avatar: MultipartPartPayload? = null,
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean get() = account.isNotBlank() && password.isNotBlank() && confirmPassword == password && !isLoading
}

sealed class RegisterEffect {
    data object RequestStoragePermission : RegisterEffect()
    data object NavigateToMain : RegisterEffect()
    data object NavigateToLogin : RegisterEffect()
    data class ShowToast(val message: String) : RegisterEffect()
}
