package com.magicvector.viewModel.activity

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mine-Video 页面 ViewModel（MVI）：
 * - 承载云录播列表、本地播放、上传断点续传三类逻辑。
 */
class MineVideoVm : ViewModel() {

    /** MVI: UI 渲染状态。 */
    private val _uiState = MutableStateFlow(MineVideoState())
    val uiState: StateFlow<MineVideoState> = _uiState.asStateFlow()

    /** MVI: 数据状态（请求上下文）。 */
    private val _dataState = MutableStateFlow(MineVideoDataState())
    val dataState: StateFlow<MineVideoDataState> = _dataState.asStateFlow()

    /** MVI: 一次性副作用。 */
    private val _effect = Channel<MineVideoEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val videoManager = MineVideoManager()
    private val uploadManager = MineUploadManager()
    private val uploadController = MineUploadController(uploadManager)

    fun processIntent(intent: MineVideoIntent) {
        when (intent) {
            MineVideoIntent.Initialize -> initialize()
            is MineVideoIntent.SwitchVideoTab -> _uiState.update { it.copy(videoTab = intent.tab) }
            MineVideoIntent.PickLocalVideoClick -> sendEffect(MineVideoEffect.OpenLocalVideoPicker)
            is MineVideoIntent.OnLocalVideoSelected -> _uiState.update {
                it.copy(selectedVideoUri = intent.uriString, isVideoPlaying = intent.uriString != null)
            }
            MineVideoIntent.ToggleVideoPlay -> _uiState.update { it.copy(isVideoPlaying = !it.isVideoPlaying) }
            MineVideoIntent.LoadCloudVideos -> loadCloudVideos()
            is MineVideoIntent.OpenCloudVideo -> openCloudVideo(intent.videoId)
            is MineVideoIntent.StartUpload -> startUpload(intent.contentResolver, intent.uri, intent.fileName)
            MineVideoIntent.PauseUpload -> uploadController.pauseTask()
            is MineVideoIntent.ResumeUpload -> {
                uploadController.resumeTask(
                    intent.contentResolver,
                    intent.uri,
                    intent.fileName,
                    _dataState.value.userId,
                    ::onUploadProgress
                )
            }
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            val user = MainApplication.getUserManager().getCurrentUser()
            val userId = MainApplication.getUserId()
            _dataState.value = _dataState.value.copy(userId = userId)
            _uiState.value = _uiState.value.copy(
                userName = if (user?.account.isNullOrBlank()) "本地用户" else user?.account.orEmpty()
            )
        }
        loadCloudVideos()
    }

    private fun loadCloudVideos() {
        val userId = _dataState.value.userId.ifBlank { MainApplication.getUserId() }
        if (userId.isBlank()) {
            return
        }
        viewModelScope.launch {
            try {
                val list = videoManager.fetchCloudRecordList(
                    userId = userId,
                    page = 1,
                    size = 30
                )
                _uiState.value = _uiState.value.copy(
                    cloudVideos = list.videos.orEmpty().map { item ->
                        MineCloudVideoItemState(
                            videoId = item.videoId?.toString().orEmpty(),
                            title = item.objectName ?: "video-${item.videoId}",
                            status = item.status ?: "UNKNOWN"
                        )
                    }
                )
            } catch (_: Throwable) {
                sendEffect(MineVideoEffect.ShowToast("获取云录播失败"))
            }
        }
    }

    private fun openCloudVideo(videoId: String) {
        viewModelScope.launch {
            try {
                val url = videoManager.resolveCloudPlayUrl(videoId = videoId)
                if (url.playUrl.isNullOrBlank()) {
                    sendEffect(MineVideoEffect.ShowToast("播放地址为空"))
                    return@launch
                }
                _uiState.value = _uiState.value.copy(cloudPlayUrl = url.playUrl)
            } catch (_: Throwable) {
                sendEffect(MineVideoEffect.ShowToast("获取播放地址失败"))
            }
        }
    }

    private fun startUpload(contentResolver: ContentResolver, uri: Uri, fileName: String) {
        val userId = _dataState.value.userId.ifBlank { MainApplication.getUserId() }
        if (userId.isBlank()) {
            sendEffect(MineVideoEffect.ShowToast("用户未登录"))
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
        _uiState.value = _uiState.value.copy(
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

    private fun sendEffect(effect: MineVideoEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

}

/** Mine-Video 页面 Intent。 */
sealed class MineVideoIntent {
    data object Initialize : MineVideoIntent()
    data class SwitchVideoTab(val tab: MineVideoTabState) : MineVideoIntent()
    data object PickLocalVideoClick : MineVideoIntent()
    data class OnLocalVideoSelected(val uriString: String?) : MineVideoIntent()
    data object ToggleVideoPlay : MineVideoIntent()
    data object LoadCloudVideos : MineVideoIntent()
    data class OpenCloudVideo(val videoId: String) : MineVideoIntent()
    data class StartUpload(val contentResolver: ContentResolver, val uri: Uri, val fileName: String) : MineVideoIntent()
    data object PauseUpload : MineVideoIntent()
    data class ResumeUpload(val contentResolver: ContentResolver, val uri: Uri, val fileName: String) : MineVideoIntent()
}

/** Mine-Video 页面 UI 状态。 */
data class MineVideoState(
    /** 用户展示名。 */
    val userName: String = "",
    /** 视频子页签。 */
    val videoTab: MineVideoTabState = MineVideoTabState.LOCAL,
    /** 本地视频 Uri。 */
    val selectedVideoUri: String? = null,
    /** 是否正在播放。 */
    val isVideoPlaying: Boolean = false,
    /** 云录播列表。 */
    val cloudVideos: List<MineCloudVideoItemState> = emptyList(),
    /** 云播放地址。 */
    val cloudPlayUrl: String? = null,
    /** 上传进度(0-1)。 */
    val uploadProgress: Float = 0f,
    /** 上传状态文本。 */
    val uploadStatus: String = "未上传"
)

/** Mine-Video 页面数据状态。 */
data class MineVideoDataState(
    /** 当前登录用户 ID。 */
    val userId: String = ""
)

/** Mine-Video 页面副作用。 */
sealed class MineVideoEffect {
    data object OpenLocalVideoPicker : MineVideoEffect()
    data class ShowToast(val message: String) : MineVideoEffect()
}

/** Mine-Video 子页签。 */
enum class MineVideoTabState {
    CLOUD,
    LOCAL,
    UPLOAD
}

/** Mine-Video 云录播展示项。 */
data class MineCloudVideoItemState(
    val videoId: String,
    val title: String,
    val status: String
)
