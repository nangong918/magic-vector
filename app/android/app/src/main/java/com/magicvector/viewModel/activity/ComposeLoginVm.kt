package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.domain.dto.http.request.UserLoginRequest
import com.magicvector.MainApplication
import com.magicvector.domain.dto.http.response.UserAuthResponse
import com.magicvector.domain.exception.NetworkBusinessException
import com.magicvector.domain.model.UserSessionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComposeLoginVm : ViewModel() {
    companion object {
        private val remoteApiSource = MainApplication.getRemoteApiSource()
        private val userManager = MainApplication.getUserManager()
    }

    /**
     * MVI 核心定义：
     * - uiState: 页面输入与按钮可用状态
     * - dataState: 页面外业务数据缓存（账号列表、userId、accessToken）
     * - intent: 用户动作
     * - effect: 页面副作用（导航/Toast）
     * - event: 当前无事件订阅
     */
    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    private val _dataState = MutableStateFlow(LoginDataState())
    val dataState: StateFlow<LoginDataState> = _dataState.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    init {
        loadSavedAccounts()
    }

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateAccount -> updateAccount(intent.account)
            is LoginIntent.UpdatePassword -> updatePassword(intent.password)
            is LoginIntent.SelectSavedAccount -> selectSavedAccount(intent.account)
            LoginIntent.SubmitLogin -> submitLogin()
            LoginIntent.NavigateToRegister -> sendEffect(LoginEffect.NavigateToRegister)
        }
    }

    private fun updateAccount(account: String) {
        _uiState.update { it.copy(account = account) }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    private fun selectSavedAccount(account: String) {
        val selected = _dataState.value.savedUserSessions.firstOrNull { it.account == account } ?: return
        _uiState.update {
            it.copy(
                account = selected.account,
                password = selected.password
            )
        }
    }

    private fun loadSavedAccounts() {
        // 默认在主线程
        viewModelScope.launch {
            // 加载数据在 IO 线程
            val sessions = withContext(Dispatchers.IO) {
                userManager.getAllUsers()
            }

            // 更新 UI 状态 - 已经在主线程
            _dataState.update { it.copy(savedUserSessions = sessions) }

            sessions.firstOrNull()?.let { first ->
                _uiState.update { current ->
                    current.copy(
                        account = current.account.ifBlank { first.account },
                        password = current.password.ifBlank { first.password }
                    )
                }
            }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(LoginEffect.ShowToast("请输入账号和密码"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        val passwordToSave = state.password
        val request = UserLoginRequest().apply {
            account = state.account.trim()
            password = passwordToSave
        }
        viewModelScope.launch {
            try {
                val auth = remoteApiSource.login(request)
                handleLoginResponse(auth, passwordToSave)
            } catch (e: NetworkBusinessException) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginEffect.ShowToast(e.msg ?: "登录失败"))
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginEffect.ShowToast("网络异常，请稍后再试"))
            }
        }
    }

    private fun handleLoginResponse(
        auth: UserAuthResponse,
        loginPassword: String
    ) {
        if (auth.userId == null || auth.userId <= 0L) {
            _uiState.update { it.copy(isLoading = false) }
            sendEffect(LoginEffect.ShowToast("登录失败"))
            return
        }

        _dataState.update {
            it.copy(
                userId = auth.userId ?: 0L,
                accessToken = auth.accessToken.orEmpty()
            )
        }

        viewModelScope.launch {
            userManager.saveCurrentUser(
                UserSessionModel(
                    userId = auth.userId ?: 0L,
                    account = auth.account.orEmpty(),
                    name = auth.name.orEmpty(),
                    avatarUrl = auth.avatarUrl.orEmpty(),
                    accessToken = auth.accessToken.orEmpty(),
                    password = loginPassword
                )
            )
            MainApplication.updateUserId(auth.userId ?: 0L)
            _uiState.update { it.copy(isLoading = false) }
            loadSavedAccounts()
            sendEffect(LoginEffect.NavigateToMain)
        }
    }

    private fun sendEffect(effect: LoginEffect) {
        // 保证effect都是在main线程被处理
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

sealed class LoginIntent {
    // 页面输入账号
    data class UpdateAccount(val account: String) : LoginIntent()
    // 页面输入密码
    data class UpdatePassword(val password: String) : LoginIntent()
    // 从本地历史账号中选择
    data class SelectSavedAccount(val account: String) : LoginIntent()
    // 提交登录
    data object SubmitLogin : LoginIntent()
    // 跳转注册
    data object NavigateToRegister : LoginIntent()
}

data class LoginState(
    // 账号输入
    val account: String = "",
    // 密码输入
    val password: String = "",
    // 登录加载状态
    val isLoading: Boolean = false
) {
    // 表单是否可提交
    val canSubmit: Boolean
        get() = account.isNotBlank() && password.isNotBlank() && !isLoading
}

data class LoginDataState(
    // 登录后的用户ID
    val userId: Long = 0L,
    // 登录后的访问令牌
    val accessToken: String = "",
    // 本地会话缓存（数据库加载），用于账号下拉与密码自动回填
    val savedUserSessions: List<UserSessionModel> = emptyList()
)

sealed class LoginEffect {
    // 导航主页面
    data object NavigateToMain : LoginEffect()
    // 导航注册页面
    data object NavigateToRegister : LoginEffect()
    // 页面提示
    data class ShowToast(val message: String) : LoginEffect()
}

