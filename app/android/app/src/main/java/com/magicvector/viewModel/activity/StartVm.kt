package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.BaseConstant
import com.data.domain.dto.request.UserTokenVerifyRequest
import com.data.domain.dto.response.UserTokenVerifyResponse
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class StartVm : ViewModel() {
    companion object {
        val TAG: String = StartVm::class.java.name
        private val api = MainApplication.getApiRequestImplInstance()
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
        // suspend的定义，线程可以不用等待，可以先去运行其他代码
        // 挂起点1：调用 suspend 函数 getCurrentUser()
        // 协程挂起，直到数据库返回结果，线程不阻塞
        val localUser = userManager.getCurrentUser()

        // 只有挂起点1完成，才会执行到这里
        // suspendCoroutine 是一个 suspend 函数，调用它的瞬间，当前协程就会主动挂起，需要continuation.resume()恢复
        return suspendCoroutine { continuation ->
            if (localUser == null || localUser.userId <= 0L) {
                continuation.resume(false)
                return@suspendCoroutine
            }
            val request = UserTokenVerifyRequest().apply {
                this.userId = localUser.userId
                this.accessToken = accessToken
            }
            api.verifyAccessToken(
                request = request,
                onSuccessCallback = object : OnSuccessCallback<BaseResponse<UserTokenVerifyResponse>> {
                    override fun onResponse(response: BaseResponse<UserTokenVerifyResponse>?) {
                        val isSuccessCode = response?.code == BaseConstant.NetworkCode.SUCCESS_CODE
                        val isValid = response?.data?.valid == true
                        // 网络请求发出后，suspendCoroutine 代码块执行完毕，但协程仍处于挂起状态
                        // 直到回调里调用 continuation.resume()，协程才恢复
                        continuation.resume(isSuccessCode && isValid)
                    }
                },
                throwableCallback = object : OnThrowableCallback {
                    override fun callback(throwable: Throwable?) {
                        // 网络请求发出后，suspendCoroutine 代码块执行完毕，但协程仍处于挂起状态
                        // 直到回调里调用 continuation.resume()，协程才恢复
                        continuation.resume(false)
                    }
                }
            )
        }
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



