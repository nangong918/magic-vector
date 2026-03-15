package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.constant.BaseConstant
import com.magicvector.MainApplication
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
        private val remoteApiSource = MainApplication.getRemoteApiSource()
        private val userManager = MainApplication.getUserManager()
    }


    //--------------------State--------------------

    /**
     * MVI 设计：
     * - uiState: 启动页展示状态
     * - dataState: 启动鉴权中间数据（是否已登录）
     * - intent: 外部事件入口
     * - effect: 页面副作用（导航）
     * - event: 当前无事件订阅
     */
    private val _uiState = MutableStateFlow(StartState())
    val uiState: StateFlow<StartState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(StartDataState())
    val dataState: StateFlow<StartDataState> = _dataState.asStateFlow()
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
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val targetEffect = resolveStartTargetEffect()
            val elapsed = System.currentTimeMillis() - startTime
            val needDelay = (BaseConstant.Constant.START_DELAY_TIME - elapsed).coerceAtLeast(0L)
            delay(needDelay)
            _uiState.update { it.copy(isLoading = false) }
            sendEffect(targetEffect)
        }
    }

    private suspend fun resolveStartTargetEffect(): StartEffect {
        val localUser = userManager.getCurrentUser()
        if (localUser == null || localUser.accessToken.isBlank()) {
            MainApplication.clearUserId()
            _dataState.update { it.copy(isLoggedIn = false, userId = 0L) }
            return StartEffect.NavigateToLogin
        }

        return try {
            val isValid = verifyAccessToken(localUser.accessToken)
            if (isValid) {
                MainApplication.updateUserId(localUser.userId)
                _dataState.update {
                    it.copy(
                        isLoggedIn = true,
                        userId = localUser.userId,
                        accessToken = localUser.accessToken
                    )
                }
                StartEffect.NavigateToMain
            } else {
                userManager.clearCurrentUser()
                MainApplication.clearUserId()
                _dataState.update { it.copy(isLoggedIn = false, userId = 0L) }
                StartEffect.NavigateToLogin
            }
        } catch (_: Throwable) {
            userManager.clearCurrentUser()
            MainApplication.clearUserId()
            _dataState.update { it.copy(isLoggedIn = false, userId = 0L) }
            StartEffect.NavigateToLogin
        }
    }

    private suspend fun verifyAccessToken(accessToken: String): Boolean {
        var verifyResult = false
        remoteApiSource.verifyAccessToken(
            accessToken = accessToken,
            handleVerifyAccessToken = { isValid ->
                verifyResult = isValid
            }
        )
        return verifyResult
    }
}

sealed class StartIntent {
    // 触发初始化鉴权
    object Initialize : StartIntent()
}

data class StartState(
    // 启动页加载状态
    val isLoading: Boolean = false,
    // 启动页倒计时状态（用于动画扩展）
    val isCountingDown: Boolean = true
)

data class StartDataState(
    // 登录态
    val isLoggedIn: Boolean = false,
    // 登录用户ID
    val userId: Long = 0L,
    // 当前 access_token
    val accessToken: String = ""
)

sealed class StartEffect {
    // 导航到主页
    object NavigateToMain : StartEffect()
    // 导航到登录页
    object NavigateToLogin : StartEffect()
}



