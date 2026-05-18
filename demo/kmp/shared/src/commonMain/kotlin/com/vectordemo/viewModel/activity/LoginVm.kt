package com.vectordemo.viewModel.activity

import com.vectordemo.di.AppContainer
import com.vectordemo.domain.model.UserSessionModel
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginVm : BaseVm() {
    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(LoginDataState())
    val dataState: StateFlow<LoginDataState> = _dataState.asStateFlow()
    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    init { loadSavedAccounts() }

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateAccount -> _uiState.update { it.copy(account = intent.account) }
            is LoginIntent.UpdatePassword -> _uiState.update { it.copy(password = intent.password) }
            is LoginIntent.SelectSavedAccount -> selectSavedAccount(intent.account)
            LoginIntent.SubmitLogin -> submitLogin()
            LoginIntent.NavigateToRegister -> sendEffect(LoginEffect.NavigateToRegister)
            LoginIntent.TouristAccess -> touristAccess()
        }
    }

    private fun loadSavedAccounts() = vmScope.launch {
        val sessions = AppContainer.userManager.getAllUsers()
        _dataState.update { it.copy(savedUserSessions = sessions) }
    }

    private fun selectSavedAccount(account: String) {
        _dataState.value.savedUserSessions.firstOrNull { it.account == account }?.let { hit ->
            _uiState.update { it.copy(account = hit.account, password = hit.password) }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(LoginEffect.ShowToast("请输入账号和密码"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        vmScope.launch {
            try {
                val session = AppContainer.userRemote.login(state.account.trim(), state.password)
                AppContainer.userManager.saveCurrentUser(session)
                AppContainer.updateUserId(session.userId)
                AppContainer.updateToken(session.accessToken)
                _uiState.update { it.copy(isLoading = false) }
                loadSavedAccounts()
                sendEffect(LoginEffect.NavigateToMain)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginEffect.ShowToast("登录失败"))
            }
        }
    }

    private fun touristAccess() {
        vmScope.launch {
            val tourist = UserSessionModel(
                userId = 1L,
                account = "tourist",
                name = "游客",
                avatarUrl = "",
                accessToken = "tourist",
                password = "",
            )
            AppContainer.userManager.saveCurrentUser(tourist)
            AppContainer.updateUserId(1L)
            AppContainer.updateToken("tourist")
            loadSavedAccounts()
            sendEffect(LoginEffect.NavigateToMain)
        }
    }

    private fun sendEffect(effect: LoginEffect) = vmScope.launch { _effect.send(effect) }
}

sealed class LoginIntent {
    data class UpdateAccount(val account: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data class SelectSavedAccount(val account: String) : LoginIntent()
    data object SubmitLogin : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object TouristAccess : LoginIntent()
}

data class LoginState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean get() = account.isNotBlank() && password.isNotBlank() && !isLoading
}

data class LoginDataState(
    val savedUserSessions: List<UserSessionModel> = emptyList(),
)

sealed class LoginEffect {
    data object NavigateToMain : LoginEffect()
    data object NavigateToRegister : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}
package com.vectordemo.viewModel.activity

import com.vectordemo.di.AppContainer
import com.vectordemo.domain.model.UserSessionModel
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginVm : BaseVm() {
    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(LoginDataState())
    val dataState: StateFlow<LoginDataState> = _dataState.asStateFlow()
    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    init { loadSavedAccounts() }

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateAccount -> _uiState.update { it.copy(account = intent.account) }
            is LoginIntent.UpdatePassword -> _uiState.update { it.copy(password = intent.password) }
            is LoginIntent.SelectSavedAccount -> selectSavedAccount(intent.account)
            LoginIntent.SubmitLogin -> submitLogin()
            LoginIntent.NavigateToRegister -> sendEffect(LoginEffect.NavigateToRegister)
            LoginIntent.TouristAccess -> touristAccess()
        }
    }

    private fun loadSavedAccounts() = vmScope.launch {
        val sessions = AppContainer.userManager.getAllUsers()
        _dataState.update { it.copy(savedUserSessions = sessions) }
    }

    private fun selectSavedAccount(account: String) {
        _dataState.value.savedUserSessions.firstOrNull { it.account == account }?.let { hit ->
            _uiState.update { it.copy(account = hit.account, password = hit.password) }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(LoginEffect.ShowToast("请输入账号和密码"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        vmScope.launch {
            try {
                val session = AppContainer.userRemote.login(state.account.trim(), state.password)
                AppContainer.userManager.saveCurrentUser(session)
                AppContainer.updateUserId(session.userId)
                AppContainer.updateToken(session.accessToken)
                _uiState.update { it.copy(isLoading = false) }
                loadSavedAccounts()
                sendEffect(LoginEffect.NavigateToMain)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginEffect.ShowToast("登录失败"))
            }
        }
    }

    private fun touristAccess() {
        vmScope.launch {
            val tourist = UserSessionModel(
                userId = 1L,
                account = "tourist",
                name = "游客",
                avatarUrl = "",
                accessToken = "tourist",
                password = "",
            )
            AppContainer.userManager.saveCurrentUser(tourist)
            AppContainer.updateUserId(1L)
            AppContainer.updateToken("tourist")
            loadSavedAccounts()
            sendEffect(LoginEffect.NavigateToMain)
        }
    }

    private fun sendEffect(effect: LoginEffect) = vmScope.launch { _effect.send(effect) }
}

sealed class LoginIntent {
    data class UpdateAccount(val account: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data class SelectSavedAccount(val account: String) : LoginIntent()
    data object SubmitLogin : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object TouristAccess : LoginIntent()
}

data class LoginState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean get() = account.isNotBlank() && password.isNotBlank() && !isLoading
}

data class LoginDataState(
    val savedUserSessions: List<UserSessionModel> = emptyList(),
)

sealed class LoginEffect {
    data object NavigateToMain : LoginEffect()
    data object NavigateToRegister : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}
package com.vectordemo.viewModel.activity

import com.vectordemo.di.AppContainer
import com.vectordemo.domain.model.UserSessionModel
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginVm : BaseVm() {
    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(LoginDataState())
    val dataState: StateFlow<LoginDataState> = _dataState.asStateFlow()
    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    init { loadSavedAccounts() }

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateAccount -> _uiState.update { it.copy(account = intent.account) }
            is LoginIntent.UpdatePassword -> _uiState.update { it.copy(password = intent.password) }
            is LoginIntent.SelectSavedAccount -> selectSavedAccount(intent.account)
            LoginIntent.SubmitLogin -> submitLogin()
            LoginIntent.NavigateToRegister -> sendEffect(LoginEffect.NavigateToRegister)
            LoginIntent.TouristAccess -> touristAccess()
        }
    }

    private fun loadSavedAccounts() = vmScope.launch {
        val sessions = AppContainer.userManager.getAllUsers()
        _dataState.update { it.copy(savedUserSessions = sessions) }
    }

    private fun selectSavedAccount(account: String) {
        _dataState.value.savedUserSessions.firstOrNull { it.account == account }?.let { hit ->
            _uiState.update { it.copy(account = hit.account, password = hit.password) }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(LoginEffect.ShowToast("请输入账号和密码"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        vmScope.launch {
            try {
                val session = AppContainer.userRemote.login(state.account.trim(), state.password)
                AppContainer.userManager.saveCurrentUser(session)
                AppContainer.updateUserId(session.userId)
                AppContainer.updateToken(session.accessToken)
                _uiState.update { it.copy(isLoading = false) }
                loadSavedAccounts()
                sendEffect(LoginEffect.NavigateToMain)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginEffect.ShowToast("登录失败"))
            }
        }
    }

    private fun touristAccess() {
        vmScope.launch {
            val tourist = UserSessionModel(
                userId = 1L,
                account = "tourist",
                name = "游客",
                avatarUrl = "",
                accessToken = "tourist",
                password = "",
            )
            AppContainer.userManager.saveCurrentUser(tourist)
            AppContainer.updateUserId(1L)
            AppContainer.updateToken("tourist")
            loadSavedAccounts()
            sendEffect(LoginEffect.NavigateToMain)
        }
    }

    private fun sendEffect(effect: LoginEffect) = vmScope.launch { _effect.send(effect) }
}

sealed class LoginIntent {
    data class UpdateAccount(val account: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data class SelectSavedAccount(val account: String) : LoginIntent()
    data object SubmitLogin : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object TouristAccess : LoginIntent()
}

data class LoginState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean get() = account.isNotBlank() && password.isNotBlank() && !isLoading
}

data class LoginDataState(
    val savedUserSessions: List<UserSessionModel> = emptyList(),
)

sealed class LoginEffect {
    data object NavigateToMain : LoginEffect()
    data object NavigateToRegister : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}
package com.vectordemo.viewModel.activity

import com.vectordemo.di.AppContainer
import com.vectordemo.domain.model.UserSessionModel
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginVm : BaseVm() {
    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(LoginDataState())
    val dataState: StateFlow<LoginDataState> = _dataState.asStateFlow()
    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    init { loadSavedAccounts() }

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateAccount -> _uiState.update { it.copy(account = intent.account) }
            is LoginIntent.UpdatePassword -> _uiState.update { it.copy(password = intent.password) }
            is LoginIntent.SelectSavedAccount -> selectSavedAccount(intent.account)
            LoginIntent.SubmitLogin -> submitLogin()
            LoginIntent.NavigateToRegister -> sendEffect(LoginEffect.NavigateToRegister)
            LoginIntent.TouristAccess -> touristAccess()
        }
    }

    private fun loadSavedAccounts() = vmScope.launch {
        val sessions = AppContainer.userManager.getAllUsers()
        _dataState.update { it.copy(savedUserSessions = sessions) }
    }

    private fun selectSavedAccount(account: String) {
        _dataState.value.savedUserSessions.firstOrNull { it.account == account }?.let { hit ->
            _uiState.update { it.copy(account = hit.account, password = hit.password) }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(LoginEffect.ShowToast("请输入账号和密码"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        vmScope.launch {
            try {
                val session = AppContainer.userRemote.login(state.account.trim(), state.password)
                AppContainer.userManager.saveCurrentUser(session)
                AppContainer.updateUserId(session.userId)
                AppContainer.updateToken(session.accessToken)
                _uiState.update { it.copy(isLoading = false) }
                loadSavedAccounts()
                sendEffect(LoginEffect.NavigateToMain)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginEffect.ShowToast("登录失败"))
            }
        }
    }

    private fun touristAccess() {
        vmScope.launch {
            val tourist = UserSessionModel(
                userId = 1L,
                account = "tourist",
                name = "游客",
                avatarUrl = "",
                accessToken = "tourist",
                password = "",
            )
            AppContainer.userManager.saveCurrentUser(tourist)
            AppContainer.updateUserId(1L)
            AppContainer.updateToken("tourist")
            loadSavedAccounts()
            sendEffect(LoginEffect.NavigateToMain)
        }
    }

    private fun sendEffect(effect: LoginEffect) = vmScope.launch { _effect.send(effect) }
}

sealed class LoginIntent {
    data class UpdateAccount(val account: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data class SelectSavedAccount(val account: String) : LoginIntent()
    data object SubmitLogin : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object TouristAccess : LoginIntent()
}

data class LoginState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean get() = account.isNotBlank() && password.isNotBlank() && !isLoading
}

data class LoginDataState(
    val savedUserSessions: List<UserSessionModel> = emptyList(),
)

sealed class LoginEffect {
    data object NavigateToMain : LoginEffect()
    data object NavigateToRegister : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}
package com.vectordemo.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectordemo.core.AppLocator
import com.vectordemo.domain.model.user.UserSessionModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginVm : ViewModel() {
    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(LoginDataState())
    val dataState: StateFlow<LoginDataState> = _dataState.asStateFlow()
    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    init { loadSavedAccounts() }

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateAccount -> _uiState.update { it.copy(account = intent.account) }
            is LoginIntent.UpdatePassword -> _uiState.update { it.copy(password = intent.password) }
            is LoginIntent.SelectSavedAccount -> selectSavedAccount(intent.account)
            LoginIntent.SubmitLogin -> submitLogin()
            LoginIntent.NavigateToRegister -> sendEffect(LoginEffect.NavigateToRegister)
            LoginIntent.TouristAccess -> touristAccess()
        }
    }

    private fun loadSavedAccounts() = viewModelScope.launch {
        val sessions = AppLocator.userManager.getAllUsers()
        _dataState.update { it.copy(savedUserSessions = sessions) }
    }

    private fun selectSavedAccount(account: String) {
        _dataState.value.savedUserSessions.firstOrNull { it.account == account }?.let { hit ->
            _uiState.update { it.copy(account = hit.account, password = hit.password) }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(LoginEffect.ShowToast("请输入账号和密码"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val session = AppLocator.userRemote.login(
                    state.account.trim(),
                    state.password,
                )
                AppLocator.userManager.saveCurrentUser(session)
                AppLocator.updateUserId(session.userId)
                _uiState.update { it.copy(isLoading = false) }
                loadSavedAccounts()
                sendEffect(LoginEffect.NavigateToMain)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginEffect.ShowToast("登录失败"))
            }
        }
    }

    private fun touristAccess() {
        viewModelScope.launch {
            AppLocator.userManager.saveCurrentUser(
                UserSessionModel(
                    userId = 1L,
                    account = "tourist",
                    name = "游客",
                    avatarUrl = "",
                    accessToken = "tourist",
                    password = ""
                )
            )
            AppLocator.updateUserId(1L)
            loadSavedAccounts()
            sendEffect(LoginEffect.NavigateToMain)
        }
    }

    private fun sendEffect(effect: LoginEffect) = viewModelScope.launch { _effect.send(effect) }
}

sealed class LoginIntent {
    data class UpdateAccount(val account: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data class SelectSavedAccount(val account: String) : LoginIntent()
    data object SubmitLogin : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object TouristAccess : LoginIntent()
}

data class LoginState(val account: String = "", val password: String = "", val isLoading: Boolean = false) {
    val canSubmit: Boolean get() = account.isNotBlank() && password.isNotBlank() && !isLoading
}

data class LoginDataState(
    val userId: Long = 0L,
    val accessToken: String = "",
    val savedUserSessions: List<UserSessionModel> = emptyList()
)

sealed class LoginEffect {
    data object NavigateToMain : LoginEffect()
    data object NavigateToRegister : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}
