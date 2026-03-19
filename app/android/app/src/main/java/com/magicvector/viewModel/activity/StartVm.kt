package com.magicvector.viewModel.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.constant.BaseConstant
import com.magicvector.MainApplication
import com.magicvector.domain.dto.http.response.UserTokenVerifyResponse
import com.magicvector.domain.exception.NetworkBusinessException
import com.magicvector.domain.model.user.UserSessionModel
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

    // 使用suspend标注方法，避免回调嵌套
    private suspend fun resolveStartTargetEffect(): StartEffect {
        val localUser = userManager.getCurrentUser()

        // 1. 前置校验：用户/Token为空 → 直接返回登录页
        if (localUser == null || localUser.accessToken.isBlank()) {
            clearUserState()
            return StartEffect.NavigateToLogin
        }

        // 2. 验证Token（异常直接外抛到VM层处理）
        try {
            val response = remoteApiSource.verifyAccessToken(localUser.accessToken)
            return handleVerifyAccessToken(response, localUser)
        } catch (e: NetworkBusinessException) {
            Log.e(TAG, "verifyAccessToken 业务异常: ", e)
            val toastMsg = e.msg ?: "服务器验证失败，请稍后重试"
            sendEffect(StartEffect.ShowToast(toastMsg))
        } catch (e: Throwable) {
            Log.e(TAG, "verifyAccessToken 系统异常: ", e)
            sendEffect(StartEffect.ShowToast("系统异常"))
        }
        return StartEffect.NavigateToLogin
    }

    private suspend fun handleVerifyAccessToken(response: UserTokenVerifyResponse, localUser: UserSessionModel): StartEffect {
        // 根据验证结果返回对应Effect
        return if (response.valid?: false) {
            // Token有效 → 更新登录状态，返回主页
            updateUserLoginState(localUser)
            StartEffect.NavigateToMain
        } else {
            // Token无效 → 清空状态，返回登录页
            clearUserState()
            StartEffect.NavigateToLogin
        }
    }

    private fun updateUserLoginState(localUser: UserSessionModel) {
        MainApplication.updateUserId(localUser.userId)
        _dataState.update {
            it.copy(
                isLoggedIn = true,
                userId = localUser.userId,
                accessToken = localUser.accessToken
            )
        }
    }

    private suspend fun clearUserState() {
        userManager.clearCurrentUser()
        MainApplication.clearUserId()
        _dataState.update { it.copy(isLoggedIn = false, userId = 0L) }
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
    // 弹窗提示（携带提示文案）
    data class ShowToast(val message: String) : StartEffect()
}



