package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.BaseConstant
import com.data.domain.dto.request.UserLoginRequest
import com.data.domain.dto.response.UserAuthResponse
import com.magicvector.MainApplication
import com.magicvector.manager.user.UserSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComposeLoginVm : ViewModel() {
    companion object {
        private val api = MainApplication.getApiRequestImplInstance()
        private val userManager = MainApplication.getUserManager()
    }

    /**
     * MVI 核心定义：
     * - uiState: 页面输入与按钮可用状态
     * - dataState: 登录后的用户核心数据
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

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateAccount -> updateAccount(intent.account)
            is LoginIntent.UpdatePassword -> updatePassword(intent.password)
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

    private fun submitLogin() {
        val state = _uiState.value
        if (!state.canSubmit) {
            sendEffect(LoginEffect.ShowToast("请输入账号和密码"))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        val request = UserLoginRequest().apply {
            account = state.account.trim()
            password = state.password
        }
        api.login(
            request = request,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<UserAuthResponse>> {
                override fun onResponse(response: BaseResponse<UserAuthResponse>?) {
                    handleLoginResponse(response)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEffect(LoginEffect.ShowToast("网络异常，请稍后再试"))
                }
            }
        )
    }

    private fun handleLoginResponse(response: BaseResponse<UserAuthResponse>?) {
        val isSuccess = response?.code == BaseConstant.NetworkCode.SUCCESS_CODE
        val auth = response?.data
        if (!isSuccess || auth == null || auth.userId == null || auth.userId <= 0L) {
            _uiState.update { it.copy(isLoading = false) }
            sendEffect(LoginEffect.ShowToast(response?.message ?: "登录失败"))
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
                UserSession(
                    userId = auth.userId ?: 0L,
                    account = auth.account.orEmpty(),
                    name = auth.name.orEmpty(),
                    avatarUrl = auth.avatarUrl.orEmpty(),
                    accessToken = auth.accessToken.orEmpty()
                )
            )
            _uiState.update { it.copy(isLoading = false) }
            sendEffect(LoginEffect.NavigateToMain)
        }
    }

    private fun sendEffect(effect: LoginEffect) {
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
    val accessToken: String = ""
)

sealed class LoginEffect {
    // 导航主页面
    data object NavigateToMain : LoginEffect()
    // 导航注册页面
    data object NavigateToRegister : LoginEffect()
    // 页面提示
    data class ShowToast(val message: String) : LoginEffect()
}
