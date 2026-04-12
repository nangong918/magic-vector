package com.vectordemo.viewModel.activity

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectordemo.MainApplication
import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.domain.model.user.UserSessionModel
import com.vectordemo.utils.file.FileUtil
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
import okhttp3.RequestBody.Companion.toRequestBody

class RegisterVm : ViewModel() {
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
            is RegisterIntent.AvatarSelected -> _uiState.update { it.copy(avatarUri = intent.avatarUri) }
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
                val avatarPart = createAvatarPartOrNull(MainApplication.getApp(), state.avatarUri)
                val auth = MainApplication.getRemoteApiSource().register(
                    avatar = avatarPart,
                    account = state.account.trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                    password = state.password.toRequestBody("text/plain".toMediaTypeOrNull()),
                    name = state.account.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                )
                val uid = auth.userId?.toLongOrNull() ?: 0L
                MainApplication.getUserManager().saveCurrentUser(
                    UserSessionModel(
                        userId = uid,
                        account = auth.account.orEmpty(),
                        name = auth.name.orEmpty(),
                        avatarUrl = auth.avatarUrl.orEmpty(),
                        accessToken = auth.accessToken.orEmpty()
                    )
                )
                MainApplication.updateUserId(uid)
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(RegisterEffect.NavigateToMain)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(RegisterEffect.ShowToast("注册失败"))
            }
        }
    }

    private fun createAvatarPartOrNull(context: Context, avatarUri: Uri?): MultipartBody.Part? {
        if (avatarUri == null) return null
        val imageManager = MainApplication.getImageManager() ?: return null
        val bitmap: Bitmap = imageManager.uriToBitmapMediaStore(context, avatarUri) ?: return null
        val processed = imageManager.processImage(bitmap, BaseConstant.Constant.BITMAP_MAX_SIZE_AVATAR)
        val imageFile = imageManager.bitmapToFile(processed, avatarUri, context) ?: return null
        return if (imageFile.exists()) FileUtil.createMultipartBodyPart(imageFile, "avatar") else null
    }

    private fun sendEffect(effect: RegisterEffect) = viewModelScope.launch { _effect.send(effect) }
}

sealed class RegisterIntent {
    data class UpdateAccount(val account: String) : RegisterIntent()
    data class UpdatePassword(val password: String) : RegisterIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    data object SelectAvatar : RegisterIntent()
    data class AvatarSelected(val avatarUri: Uri?) : RegisterIntent()
    data object SubmitRegister : RegisterIntent()
    data object NavigateToLogin : RegisterIntent()
}

data class RegisterState(
    val account: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val avatarUri: Uri? = null,
    val isLoading: Boolean = false
) {
    val canSubmit: Boolean get() = account.isNotBlank() && password.isNotBlank() && confirmPassword == password && !isLoading
}

sealed class RegisterEffect {
    data object RequestStoragePermission : RegisterEffect()
    data object NavigateToMain : RegisterEffect()
    data object NavigateToLogin : RegisterEffect()
    data class ShowToast(val message: String) : RegisterEffect()
}
