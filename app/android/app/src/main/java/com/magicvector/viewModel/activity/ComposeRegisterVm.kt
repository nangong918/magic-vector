package com.magicvector.viewModel.activity

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.file.FileUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.BaseConstant
import com.data.domain.dto.response.UserAuthResponse
import com.magicvector.MainApplication
import com.magicvector.domain.model.UserSessionModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ComposeRegisterVm : ViewModel() {
    companion object {
        private val api = MainApplication.getApiRequestImplInstance()
        private val userManager = MainApplication.getUserManager()
    }

    /**
     * MVI 核心定义：
     * - uiState: 页面可见输入与按钮状态
     * - dataState: 注册成功后的关键会话数据
     * - intent: 用户交互
     * - effect: 页面副作用（权限、导航、提示）
     * - event: 当前无事件订阅
     */
    private val _uiState = MutableStateFlow(RegisterState())
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    private val _dataState = MutableStateFlow(RegisterDataState())
    val dataState: StateFlow<RegisterDataState> = _dataState.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect: Flow<RegisterEffect> = _effect.receiveAsFlow()

    fun processIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateAccount -> _uiState.update { it.copy(account = intent.account) }
            is RegisterIntent.UpdatePassword -> _uiState.update { it.copy(password = intent.password) }
            is RegisterIntent.UpdateConfirmPassword -> _uiState.update { it.copy(confirmPassword = intent.confirmPassword) }
            RegisterIntent.SelectAvatar -> sendEffect(RegisterEffect.RequestStoragePermission)
            is RegisterIntent.AvatarSelected -> _uiState.update { it.copy(avatarUri = intent.avatarUri) }
            RegisterIntent.SubmitRegister -> submitRegister()
            RegisterIntent.NavigateToLogin -> sendEffect(RegisterEffect.NavigateToLogin)
        }
    }

    private fun submitRegister() {
        val state = _uiState.value
        if (state.account.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank()) {
            sendEffect(RegisterEffect.ShowToast("请填写完整信息"))
            return
        }
        if (state.password != state.confirmPassword) {
            sendEffect(RegisterEffect.ShowToast("两次密码输入不一致"))
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        sendEffect(RegisterEffect.SubmitRequest)
    }

    fun register(context: Context) {
        val state = _uiState.value
        val avatarPart = createAvatarPartOrNull(context, state.avatarUri)

        val accountBody = RequestBody.create("text/plain".toMediaTypeOrNull(), state.account.trim())
        val passwordBody = RequestBody.create("text/plain".toMediaTypeOrNull(), state.password)
        val nameBody = RequestBody.create("text/plain".toMediaTypeOrNull(), state.account.trim())

        api.register(
            avatar = avatarPart,
            account = accountBody,
            password = passwordBody,
            name = nameBody,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<UserAuthResponse>> {
                override fun onResponse(response: BaseResponse<UserAuthResponse>?) {
                    handleRegisterResponse(response)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEffect(RegisterEffect.ShowToast("网络异常，请稍后再试"))
                }
            }
        )
    }

    private fun handleRegisterResponse(response: BaseResponse<UserAuthResponse>?) {
        val isSuccess = response?.code == BaseConstant.NetworkCode.SUCCESS_CODE
        val auth = response?.data
        if (!isSuccess || auth == null || auth.userId == null || auth.userId <= 0L) {
            _uiState.update { it.copy(isLoading = false) }
            sendEffect(RegisterEffect.ShowToast(response?.message ?: "注册失败"))
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
                    password = ""
                )
            )
            MainApplication.updateUserId(auth.userId ?: 0L)
            _uiState.update { it.copy(isLoading = false) }
            sendEffect(RegisterEffect.NavigateToMain)
        }
    }

    private fun createAvatarPartOrNull(context: Context, avatarUri: Uri?): MultipartBody.Part? {
        if (avatarUri == null) {
            return null
        }
        val imageManager = MainApplication.getImageManager() ?: return null
        val bitmap: Bitmap? = imageManager.uriToBitmapMediaStore(context, avatarUri)
        val processed = imageManager.processImage(bitmap, BaseConstant.Constant.BITMAP_MAX_SIZE_AVATAR)
        val imageFile = imageManager.bitmapToFile(processed, avatarUri, context) ?: return null
        if (!imageFile.exists()) {
            return null
        }
        return FileUtil.createMultipartBodyPart(imageFile, "avatar")
    }

    private fun sendEffect(effect: RegisterEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

sealed class RegisterIntent {
    // 输入账号
    data class UpdateAccount(val account: String) : RegisterIntent()
    // 输入密码
    data class UpdatePassword(val password: String) : RegisterIntent()
    // 输入确认密码
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    // 选择头像
    data object SelectAvatar : RegisterIntent()
    // 头像选择结果
    data class AvatarSelected(val avatarUri: Uri?) : RegisterIntent()
    // 提交注册
    data object SubmitRegister : RegisterIntent()
    // 跳转登录
    data object NavigateToLogin : RegisterIntent()
}

data class RegisterState(
    // 账号输入
    val account: String = "",
    // 密码输入
    val password: String = "",
    // 确认密码输入
    val confirmPassword: String = "",
    // 头像选择结果
    val avatarUri: Uri? = null,
    // 注册加载状态
    val isLoading: Boolean = false
) {
    // 表单是否可提交
    val canSubmit: Boolean
        get() = account.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                password == confirmPassword &&
                !isLoading
}

data class RegisterDataState(
    // 注册后用户ID
    val userId: Long = 0L,
    // 注册后access_token
    val accessToken: String = ""
)

sealed class RegisterEffect {
    // 申请存储权限
    data object RequestStoragePermission : RegisterEffect()
    // 提交注册请求
    data object SubmitRequest : RegisterEffect()
    // 导航主页
    data object NavigateToMain : RegisterEffect()
    // 导航登录页
    data object NavigateToLogin : RegisterEffect()
    // 页面提示
    data class ShowToast(val message: String) : RegisterEffect()
}
