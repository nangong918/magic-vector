package com.magicvector.viewModel.fragment

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.activity.test.ComposeTestActivity
import com.data.domain.dto.request.UserPasswordUpdateRequest
import com.magicvector.MainApplication
import com.magicvector.manager.mine.MineUploadController
import com.magicvector.manager.mine.MineUploadManager
import com.magicvector.manager.mine.MineUploadProgress
import com.magicvector.manager.mine.MineVideoManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MineVm : ViewModel() {

    /** MVI: UI 渲染状态。 */
    private val _uiState = MutableStateFlow(MineState())
    val uiState: StateFlow<MineState> = _uiState.asStateFlow()

    /** MVI: 一次性副作用。 */
    private val _effect = Channel<MineEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private val api = MainApplication.getApiRequestImplInstance()
    private val videoManager = MineVideoManager()
    private val uploadManager = MineUploadManager()
    private val uploadController = MineUploadController(uploadManager)

    fun processIntent(intent: MineIntent) {
        when (intent) {
            MineIntent.Initialize -> initialize()
            MineIntent.TestButtonClick -> sendEffect(MineEffect.NavigateToActivity(ComposeTestActivity::class.java.name))
            MineIntent.SwitchToSetting -> _uiState.update { it.copy(tab = MineMainTab.SETTING) }
            MineIntent.SwitchToVideo -> _uiState.update { it.copy(tab = MineMainTab.VIDEO) }
            is MineIntent.SwitchVideoTab -> _uiState.update { it.copy(videoTab = intent.tab) }
            MineIntent.PickLocalVideoClick -> sendEffect(MineEffect.OpenLocalVideoPicker)
            is MineIntent.OnLocalVideoSelected -> _uiState.update {
                it.copy(selectedVideoUri = intent.uriString, isVideoPlaying = intent.uriString != null)
            }
            MineIntent.ToggleVideoPlay -> _uiState.update { it.copy(isVideoPlaying = !it.isVideoPlaying) }
            is MineIntent.UpdateOldPassword -> _uiState.update { it.copy(oldPassword = intent.value) }
            is MineIntent.UpdateNewPassword -> _uiState.update { it.copy(newPassword = intent.value) }
            MineIntent.SubmitPasswordUpdate -> submitPasswordUpdate()
            MineIntent.Logout -> logout()
            MineIntent.LoadCloudVideos -> loadCloudVideos()
            is MineIntent.OpenCloudVideo -> openCloudVideo(intent.videoId)
            is MineIntent.StartUpload -> startUpload(intent.contentResolver, intent.uri, intent.fileName)
            MineIntent.PauseUpload -> uploadController.pauseTask()
            is MineIntent.ResumeUpload -> uploadController.resumeTask(intent.contentResolver, intent.uri, intent.fileName, MainApplication.getUserId(), ::onUploadProgress)
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            val user = MainApplication.getUserManager().getCurrentUser()
            _uiState.update {
                it.copy(
                    userName = user?.account ?: "本地用户",
                    settingTodo = "Setting 已接入修改密码与登出",
                    cloudReplayTodo = "云录播列表+播放URL骨架已接入",
                    uploadTodo = "上传断点续传骨架已接入"
                )
            }
        }
        loadCloudVideos()
    }

    private fun submitPasswordUpdate() {
        val old = _uiState.value.oldPassword
        val new = _uiState.value.newPassword
        if (old.isBlank() || new.isBlank()) {
            sendEffect(MineEffect.ShowToast("请输入旧密码和新密码"))
            return
        }
        val request = UserPasswordUpdateRequest().apply {
            userId = MainApplication.getUserId()
            oldPassword = old
            newPassword = new
        }
        api.updatePassword(
            request = request,
            onSuccessCallback = { response ->
                val ok = response?.data?.updated == true
                sendEffect(MineEffect.ShowToast(if (ok) "密码修改成功" else response?.data?.message ?: "修改失败"))
            },
            throwableCallback = {
                sendEffect(MineEffect.ShowToast("修改密码失败"))
            }
        )
    }

    private fun logout() {
        viewModelScope.launch {
            MainApplication.getUserManager().clearCurrentUser()
            MainApplication.clearUserId()
            sendEffect(MineEffect.NavigateToLogin)
        }
    }

    private fun loadCloudVideos() {
        val userId = MainApplication.getUserId()
        if (userId.isBlank()) {
            return
        }
        videoManager.fetchCloudRecordList(
            userId = userId,
            page = 1,
            size = 30,
            onSuccess = { list ->
                _uiState.update {
                    it.copy(cloudVideos = list?.videos.orEmpty().map { item ->
                        MineCloudVideoItem(
                            videoId = item.videoId?.toString().orEmpty(),
                            title = item.objectName ?: "video-${item.videoId}",
                            status = item.status ?: "UNKNOWN"
                        )
                    })
                }
            },
            onError = {
                sendEffect(MineEffect.ShowToast("获取云录播失败"))
            }
        )
    }

    private fun openCloudVideo(videoId: String) {
        videoManager.resolveCloudPlayUrl(
            videoId = videoId,
            onSuccess = { url ->
                if (url?.playUrl.isNullOrBlank()) {
                    sendEffect(MineEffect.ShowToast("播放地址为空"))
                    return@resolveCloudPlayUrl
                }
                _uiState.update {
                    it.copy(cloudPlayUrl = url?.playUrl)
                }
            },
            onError = {
                sendEffect(MineEffect.ShowToast("获取播放地址失败"))
            }
        )
    }

    private fun startUpload(contentResolver: ContentResolver, uri: Uri, fileName: String) {
        val userId = MainApplication.getUserId()
        if (userId.isBlank()) {
            sendEffect(MineEffect.ShowToast("用户未登录"))
            return
        }
        uploadController.scheduleUpload(
            contentResolver = contentResolver,
            uri = uri,
            fileName = fileName,
            userId = userId,
            callback = ::onUploadProgress
        )
    }

    private fun onUploadProgress(progress: MineUploadProgress) {
        _uiState.update {
            it.copy(
                uploadProgress = if (progress.totalBytes > 0) {
                    progress.uploadedBytes.toFloat() / progress.totalBytes.toFloat()
                } else 0f,
                uploadStatus = when {
                    progress.completed -> "上传完成"
                    progress.paused -> "已暂停"
                    !progress.error.isNullOrBlank() -> progress.error
                    else -> "上传中 ${progress.uploadedBytes}/${progress.totalBytes}"
                }
            )
        }
    }

    private fun sendEffect(effect: MineEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private fun MutableStateFlow<MineState>.update(block: (MineState) -> MineState) {
        value = block(value)
    }
}

sealed class MineIntent {
    data object Initialize : MineIntent()
    data object TestButtonClick : MineIntent()
    data object SwitchToSetting : MineIntent()
    data object SwitchToVideo : MineIntent()
    data class SwitchVideoTab(val tab: MineVideoTab) : MineIntent()
    data object PickLocalVideoClick : MineIntent()
    data class OnLocalVideoSelected(val uriString: String?) : MineIntent()
    data object ToggleVideoPlay : MineIntent()
    data class UpdateOldPassword(val value: String) : MineIntent()
    data class UpdateNewPassword(val value: String) : MineIntent()
    data object SubmitPasswordUpdate : MineIntent()
    data object Logout : MineIntent()
    data object LoadCloudVideos : MineIntent()
    data class OpenCloudVideo(val videoId: String) : MineIntent()
    data class StartUpload(val contentResolver: ContentResolver, val uri: Uri, val fileName: String) : MineIntent()
    data object PauseUpload : MineIntent()
    data class ResumeUpload(val contentResolver: ContentResolver, val uri: Uri, val fileName: String) : MineIntent()
}

data class MineState(
    /** 页面昵称。 */
    val userName: String = "",
    /** 当前一级Tab。 */
    val tab: MineMainTab = MineMainTab.VIDEO,
    /** 当前视频子Tab。 */
    val videoTab: MineVideoTab = MineVideoTab.LOCAL,
    /** 本地视频 Uri。 */
    val selectedVideoUri: String? = null,
    /** 当前是否播放。 */
    val isVideoPlaying: Boolean = false,
    /** 云视频列表。 */
    val cloudVideos: List<MineCloudVideoItem> = emptyList(),
    /** 云播放地址。 */
    val cloudPlayUrl: String? = null,
    /** 上传进度(0-1)。 */
    val uploadProgress: Float = 0f,
    /** 上传状态文本。 */
    val uploadStatus: String = "未上传",
    /** 旧密码输入。 */
    val oldPassword: String = "",
    /** 新密码输入。 */
    val newPassword: String = "",
    /** 设置模块 TODO。 */
    val settingTodo: String = "",
    /** 云录播 TODO。 */
    val cloudReplayTodo: String = "",
    /** 上传云端 TODO。 */
    val uploadTodo: String = ""
)

sealed class MineEffect {
    data object OpenLocalVideoPicker : MineEffect()
    data class NavigateToActivity(val activityClassName: String) : MineEffect()
    data object NavigateToLogin : MineEffect()
    data class ShowToast(val message: String) : MineEffect()
}

enum class MineMainTab {
    SETTING,
    VIDEO
}

enum class MineVideoTab {
    CLOUD,
    LOCAL,
    UPLOAD
}

data class MineCloudVideoItem(
    val videoId: String,
    val title: String,
    val status: String
)