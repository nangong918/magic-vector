package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.magicvector.domain.dto.http.request.UserPasswordUpdateRequest
import com.magicvector.domain.dto.http.response.UserPasswordUpdateResponse
import com.magicvector.MainApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mine-Setting 页面 ViewModel（MVI）：
 * - 负责设置页输入状态与提交副作用，不承载入口导航逻辑。
 */
class ComposeMineSettingVm : ViewModel() {

    /** MVI: UI 渲染状态（输入框/昵称等）。 */
    private val _uiState = MutableStateFlow(MineSettingState())
    val uiState: StateFlow<MineSettingState> = _uiState.asStateFlow()

    /** MVI: 数据状态（不直接参与渲染）。 */
    private val _dataState = MutableStateFlow(MineSettingDataState())
    val dataState: StateFlow<MineSettingDataState> = _dataState.asStateFlow()

    /** MVI: 一次性副作用（Toast、跳转）。 */
    private val _effect = Channel<MineSettingEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val remoteApiSource = MainApplication.getRemoteApiSource()

    fun processIntent(intent: MineSettingIntent) {
        when (intent) {
            MineSettingIntent.Initialize -> initialize()
            is MineSettingIntent.UpdateOldPassword -> _uiState.update { it.copy(oldPassword = intent.value) }
            is MineSettingIntent.UpdateNewPassword -> _uiState.update { it.copy(newPassword = intent.value) }
            MineSettingIntent.SubmitPasswordUpdate -> submitPasswordUpdate()
            MineSettingIntent.Logout -> logout()
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            val user = MainApplication.getUserManager().getCurrentUser()
            _dataState.value = _dataState.value.copy(
                    userId = MainApplication.getUserId(),
                    account = user?.account.orEmpty()
                )
            _uiState.value = _uiState.value.copy(
                    userName = if (user?.account.isNullOrBlank()) "本地用户" else user?.account.orEmpty()
                )
        }
    }

    private fun submitPasswordUpdate() {
        val old = _uiState.value.oldPassword
        val new = _uiState.value.newPassword
        if (old.isBlank() || new.isBlank()) {
            sendEffect(MineSettingEffect.ShowToast("请输入旧密码和新密码"))
            return
        }
        val userId = _dataState.value.userId.ifBlank { MainApplication.getUserId() }
        if (userId.isBlank()) {
            sendEffect(MineSettingEffect.ShowToast("用户未登录"))
            return
        }
        val request = UserPasswordUpdateRequest().apply {
            this.userId = userId
            this.oldPassword = old
            this.newPassword = new
        }
        remoteApiSource.updatePassword(
            request = request,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<UserPasswordUpdateResponse>> {
                override fun onResponse(response: BaseResponse<UserPasswordUpdateResponse>?) {
                    val ok = response?.data?.updated == true
                    sendEffect(
                        MineSettingEffect.ShowToast(
                            if (ok) "密码修改成功" else (response?.data?.message ?: "修改失败")
                        )
                    )
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    sendEffect(MineSettingEffect.ShowToast("修改密码失败"))
                }
            }
        )
    }

    private fun logout() {
        viewModelScope.launch {
            MainApplication.getUserManager().clearCurrentUser()
            MainApplication.clearUserId()
            sendEffect(MineSettingEffect.NavigateToLogin)
        }
    }

    private fun sendEffect(effect: MineSettingEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

}

/** Mine-Setting 页面 Intent。 */
sealed class MineSettingIntent {
    data object Initialize : MineSettingIntent()
    data class UpdateOldPassword(val value: String) : MineSettingIntent()
    data class UpdateNewPassword(val value: String) : MineSettingIntent()
    data object SubmitPasswordUpdate : MineSettingIntent()
    data object Logout : MineSettingIntent()
}

/** Mine-Setting 页面 UI 状态。 */
data class MineSettingState(
    /** 用户展示名。 */
    val userName: String = "",
    /** 旧密码输入。 */
    val oldPassword: String = "",
    /** 新密码输入。 */
    val newPassword: String = ""
)

/** Mine-Setting 页面数据状态。 */
data class MineSettingDataState(
    /** 当前登录用户 ID。 */
    val userId: String = "",
    /** 当前账号（不直接渲染核心逻辑）。 */
    val account: String = ""
)

/** Mine-Setting 页面副作用。 */
sealed class MineSettingEffect {
    data object NavigateToLogin : MineSettingEffect()
    data class ShowToast(val message: String) : MineSettingEffect()
}
