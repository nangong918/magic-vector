package com.magicvector.viewModel.activity

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.file.FileUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.core.baseutil.ui.ToastUtils
import com.data.domain.constant.BaseConstant
import com.magicvector.domain.dto.http.response.AgentResponse
import com.magicvector.MainApplication
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
import java.io.File


class ComposeCreateAgentVm() : ViewModel() {

    companion object {
        val TAG: String = ComposeCreateAgentVm::class.java.name
        private const val MAX_NAME_LENGTH = BaseConstant.Constant.MAX_AGENT_NAME_LENGTH
        val api = MainApplication.getApiRequestImplInstance()
    }

    // State
    private val _uiState = MutableStateFlow(CreateAgentState())
    val uiState: StateFlow<CreateAgentState> = _uiState.asStateFlow()

    // Effect
    private val _effect = Channel<CreateAgentEffect>(Channel.BUFFERED)
    val effect: Flow<CreateAgentEffect> = _effect.receiveAsFlow()


    fun processIntent(intent: CreateAgentIntent) {
        when (intent) {
            is CreateAgentIntent.Initialize -> {
                initialize()
            }
            is CreateAgentIntent.UpdateAgentName -> {
                updateAgentName(intent.name)
            }
            is CreateAgentIntent.UpdateAgentDescription -> {
                updateAgentDescription(intent.description)
            }
            is CreateAgentIntent.SelectAvatar -> {
                selectAvatar()
            }
            is CreateAgentIntent.AvatarSelected -> {
                updateAvatar(intent.uri)
            }
            is CreateAgentIntent.SubmitCreate -> {
                submitCreate()
            }
            is CreateAgentIntent.CancelCreate -> {
                cancelCreate()
            }
        }
    }


    private fun initialize() {
        _uiState.update {
            it.copy(isLoading = false)
        }
    }

    private fun updateAgentName(name: String) {
        val (isValid, error) = validateName(name)
        _uiState.update {
            it.copy(
                agentName = name,
                isNameValid = isValid,
                nameError = error
            )
        }
    }

    private fun updateAgentDescription(description: String) {
        val (isValid, error) = validateDescription(description)
        _uiState.update {
            it.copy(
                agentDescription = description,
                isDescriptionValid = isValid,
                descriptionError = error
            )
        }
    }

    private fun updateAvatar(uri: Uri?) {
        _uiState.update {
            it.copy(avatarUri = uri)
        }
    }

    private fun selectAvatar() {
        // 请求权限，通过 Effect 让 UI 层处理
        sendEffect(CreateAgentEffect.RequestStoragePermission)
    }


    // 权限请求结果回调（由 UI 层调用）
    fun onPermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            // 权限已授予，打开图片选择器
            sendEffect(CreateAgentEffect.NavigateToImagePicker())
        } else {
            // 权限被拒绝，显示提示
            sendEffect(CreateAgentEffect.ShowError(
                context.getString(com.view.appview.R.string.please_give_permission)
            ))
        }
    }

    private fun submitCreate() {
        val state = _uiState.value

        // 表单验证
        if (!state.isFormValid) {
            if (!state.isNameValid) {
                sendEffect(CreateAgentEffect.ShowError(state.nameError ?: "请填写正确的Agent名称"))
            } else if (!state.isDescriptionValid) {
                sendEffect(CreateAgentEffect.ShowError(state.descriptionError ?: "请填写正确的Agent描述"))
            }
            return
        }

        sendEffect(CreateAgentEffect.CreateAgent)
    }

    fun createAgent(context: Context) {
        val state = _uiState.value

        var filePart: MultipartBody.Part? = null

        state.avatarUri.let {
            uri ->
            var bitmap: Bitmap? = MainApplication.getImageManager()!!.
            uriToBitmapMediaStore(context, uri)
            // 裁剪
            bitmap = MainApplication.getImageManager()!!.
            processImage(bitmap, BaseConstant.Constant.BITMAP_MAX_SIZE_AVATAR)

            // Http Send
            var imageFile: File? = null

            imageFile = MainApplication.getImageManager()!!.
            bitmapToFile(bitmap, uri, context)

            if (imageFile == null || !imageFile.exists()) {
                // 处理文件未创建或路径不正确的情况
                Log.w(TAG, "Image file creation failed")
                return
            }

            // 获取文件名
            // 使用 getName() 获取文件名
            val originalFilename = imageFile.name
            Log.d(TAG, "Original Filename: $originalFilename")
            filePart = FileUtil.createMultipartBodyPart(imageFile, "img")
        }

        val userIdBody = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            MainApplication.getUserId()
        )
        val nameBody = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            state.agentName
        )
        val descriptionBody = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            state.agentDescription
        )

        api.createAgent(
            filePart,
            userIdBody,
            nameBody,
            descriptionBody,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<AgentResponse>> {
                override fun onResponse(response: BaseResponse<AgentResponse>?) {
                    response?.let {
                        if (response.data?.agentAo?.agentId != null){
                            ToastUtils.showToastActivity(context, context.getString(
                                com.view.appview.R.string.create_success
                            ))
                            _uiState.update {
                                it.copy(isCreateSuccess = false)
                            }
                        }
                    }
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    Log.e(TAG, "Create agent failed: ", throwable)
                    sendEffect(CreateAgentEffect.ShowError(
                        context.getString(com.view.appview.R.string.create_failed)
                    ))
                }
            }
        )
    }

    private fun cancelCreate() {
        sendEffect(CreateAgentEffect.NavigateBack)
    }

    // 验证方法
    private fun validateName(name: String): Pair<Boolean, String?> {
        val trimmedName = name.replace("\\s+".toRegex(), "")

        return when {
            name.isBlank() -> Pair(false, "Agent名称不能为空")
            trimmedName.isEmpty() -> Pair(false, "名称不能只包含空格或特殊字符")
            trimmedName.length > MAX_NAME_LENGTH -> Pair(false, "名称不能超过${MAX_NAME_LENGTH}个字符")
            !trimmedName.all { it.isLetterOrDigit() } -> Pair(false, "名称只能包含字母和数字")
            else -> Pair(true, null)
        }
    }

    private fun validateDescription(description: String): Pair<Boolean, String?> {
        val trimmedDesc = description.replace("\\s+".toRegex(), "")

        return when {
            description.isBlank() -> Pair(false, "Agent描述不能为空")
            trimmedDesc.isEmpty() -> Pair(false, "描述不能只包含空格或特殊字符")
            description.length > 500 -> Pair(false, "描述不能超过500个字符")
            else -> Pair(true, null)
        }
    }


    // 发送 Effect
    private fun sendEffect(effect: CreateAgentEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}



sealed class CreateAgentIntent {
    data object Initialize : CreateAgentIntent()

    // 输入相关
    data class UpdateAgentName(val name: String) : CreateAgentIntent()
    data class UpdateAgentDescription(val description: String) : CreateAgentIntent()

    // 头像相关
    data object SelectAvatar : CreateAgentIntent()
    data class AvatarSelected(val uri: Uri?) : CreateAgentIntent()

    // 提交相关
    data object SubmitCreate : CreateAgentIntent()
    data object CancelCreate : CreateAgentIntent()
}


@Stable
data class CreateAgentState(
    // 输入字段
    val agentName: String = "",
    val agentDescription: String = "",

    // 头像
    val avatarUri: Uri? = null,
    val defaultAvatarResId: Int = com.view.appview.R.xml.person_24px,

    // 验证状态
    val isNameValid: Boolean = false,
    val isDescriptionValid: Boolean = false,
    val nameError: String? = null,
    val descriptionError: String? = null,

    // UI状态
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,

    // 结果状态
    val isCreateSuccess: Boolean = false,
    val createdAgentId: String? = null
) {
    val isFormValid: Boolean
        get() = isNameValid && isDescriptionValid && !isSubmitting
}


sealed class CreateAgentEffect {
    // 导航相关
    data object NavigateBack : CreateAgentEffect()
    data class NavigateToImagePicker(val requestCode: Int = 100) : CreateAgentEffect()

    // 权限相关
    data object RequestStoragePermission : CreateAgentEffect()

    // 提示相关
    data class ShowToast(val message: String) : CreateAgentEffect()
    data class ShowError(val message: String) : CreateAgentEffect()

    // 创建
    data object CreateAgent : CreateAgentEffect()

    // 结果相关
    data class AgentCreated(val agentId: String) : CreateAgentEffect()
}














