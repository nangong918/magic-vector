package com.vectordemo.viewModel.oss

import com.vectordemo.di.AppContainer
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.repository.api.MultipartPartPayload
import androidx.lifecycle.viewModelScope
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OssReplacePending(val bucket: String, val fileId: Long)

data class OssDemoState(
    val touristBlocked: Boolean = false,
    val buckets: List<String> = emptyList(),
    val expandedBuckets: Set<String> = emptySet(),
    val filesByBucket: Map<String, List<OssBucketFileItemModel>> = emptyMap(),
    val loadingBuckets: Boolean = false,
    val loadingBucket: String? = null,
    val pickedUploadFile: MultipartPartPayload? = null,
    val replacePending: OssReplacePending? = null,
)

sealed class OssDemoIntent {
    data object RefreshBuckets : OssDemoIntent()
    data class ToggleBucket(val bucket: String) : OssDemoIntent()
    data class LoadBucketFiles(val bucket: String, val force: Boolean = false) : OssDemoIntent()
    data object RequestMainImagePick : OssDemoIntent()
    data class MainImagePicked(val file: MultipartPartPayload?) : OssDemoIntent()
    data object UploadSubmit : OssDemoIntent()
    data class DownloadImage(val url: String, val displayName: String) : OssDemoIntent()
    data class RequestReplacePick(val bucket: String, val fileId: Long) : OssDemoIntent()
    data class ReplaceImagePicked(val file: MultipartPartPayload?) : OssDemoIntent()
    data class DeleteFile(val bucket: String, val fileId: Long) : OssDemoIntent()
}

sealed class OssDemoEffect {
    data class ShowToast(val message: String) : OssDemoEffect()
    data object OpenMainImagePicker : OssDemoEffect()
    data object OpenReplaceImagePicker : OssDemoEffect()
}

class OssDemoVm : BaseVm() {
    private val _uiState = MutableStateFlow(OssDemoState())
    val uiState: StateFlow<OssDemoState> = _uiState.asStateFlow()

    private val _effect = Channel<OssDemoEffect>(Channel.BUFFERED)
    val effect: Flow<OssDemoEffect> = _effect.receiveAsFlow()

    init { initialize() }

    fun processIntent(intent: OssDemoIntent) {
        when (intent) {
            OssDemoIntent.RefreshBuckets -> refreshBuckets()
            is OssDemoIntent.ToggleBucket -> toggleBucket(intent.bucket)
            is OssDemoIntent.LoadBucketFiles -> loadBucketFiles(intent.bucket, intent.force)
            OssDemoIntent.RequestMainImagePick -> sendEffect(OssDemoEffect.OpenMainImagePicker)
            is OssDemoIntent.MainImagePicked -> _uiState.update { it.copy(pickedUploadFile = intent.file) }
            OssDemoIntent.UploadSubmit -> uploadImage()
            is OssDemoIntent.DownloadImage -> sendEffect(OssDemoEffect.ShowToast("KMP 版本暂未实现下载：${intent.displayName}"))
            is OssDemoIntent.RequestReplacePick -> requestReplacePick(intent.bucket, intent.fileId)
            is OssDemoIntent.ReplaceImagePicked -> replaceImagePicked(intent.file)
            is OssDemoIntent.DeleteFile -> deleteFile(intent.bucket, intent.fileId)
        }
    }

    private fun sendEffect(effect: OssDemoEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun initialize() {
        viewModelScope.launch {
            val u = AppContainer.userManager.getCurrentUser()
            val blocked = u == null || u.userId <= 0L || u.userId == 1L ||
                u.accessToken.isBlank() || u.accessToken == "tourist" || u.account == "tourist"
            _uiState.update { it.copy(touristBlocked = blocked) }
            if (!blocked) doRefreshBuckets()
        }
    }

    private fun refreshBuckets() {
        viewModelScope.launch {
            if (_uiState.value.touristBlocked) return@launch
            doRefreshBuckets()
        }
    }

    private suspend fun doRefreshBuckets() {
        val expandedBefore = _uiState.value.expandedBuckets.toSet()
        try {
            _uiState.update { it.copy(loadingBuckets = true, filesByBucket = emptyMap(), loadingBucket = null) }
            val uid = currentUserId()
            val res = AppContainer.ossManager.syncUserBucketList(uid)
            val newBuckets = res.bucketNames
            _uiState.update { it.copy(loadingBuckets = false, buckets = newBuckets) }
            expandedBefore.intersect(newBuckets.toSet()).forEach { b -> doLoadBucketFiles(b, force = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(loadingBuckets = false) }
            sendEffect(OssDemoEffect.ShowToast(e.message ?: "加载存储桶失败"))
        }
    }

    private fun toggleBucket(bucket: String) {
        val expanded = _uiState.value.expandedBuckets.toMutableSet()
        if (!expanded.add(bucket)) expanded.remove(bucket)
        _uiState.update { it.copy(expandedBuckets = expanded) }
        if (expanded.contains(bucket)) loadBucketFiles(bucket, force = false)
    }

    private fun loadBucketFiles(bucket: String, force: Boolean) {
        if (!force && _uiState.value.filesByBucket.containsKey(bucket)) return
        viewModelScope.launch {
            if (_uiState.value.touristBlocked) return@launch
            doLoadBucketFiles(bucket, force)
        }
    }

    private suspend fun doLoadBucketFiles(bucket: String, force: Boolean) {
        if (!force && _uiState.value.filesByBucket.containsKey(bucket)) return
        try {
            _uiState.update { it.copy(loadingBucket = bucket) }
            val uid = currentUserId()
            val res = AppContainer.ossManager.syncBucketFileItemList(uid, bucket)
            _uiState.update {
                val map = it.filesByBucket.toMutableMap()
                map[bucket] = res.items
                it.copy(loadingBucket = null, filesByBucket = map)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loadingBucket = null) }
            sendEffect(OssDemoEffect.ShowToast(e.message ?: "加载文件失败"))
        }
    }

    private fun invalidateBucket(bucket: String) {
        _uiState.update {
            val m = it.filesByBucket.toMutableMap()
            m.remove(bucket)
            it.copy(filesByBucket = m)
        }
        if (_uiState.value.expandedBuckets.contains(bucket)) loadBucketFiles(bucket, force = true)
    }

    private fun uploadImage() {
        viewModelScope.launch {
            if (_uiState.value.touristBlocked) return@launch
            val file = _uiState.value.pickedUploadFile
            if (file == null) {
                sendEffect(OssDemoEffect.ShowToast("请先选择图片"))
                return@launch
            }
            try {
                val uid = currentUserId()
                val res = AppContainer.ossManager.batchUploadSingle(uid, null, file)
                val ok = res.items.any { it.success }
                sendEffect(OssDemoEffect.ShowToast(if (ok) "上传成功" else res.items.firstOrNull()?.message ?: "上传失败"))
            } catch (e: Exception) {
                sendEffect(OssDemoEffect.ShowToast(e.message ?: "上传失败"))
            }
        }
    }

    private fun requestReplacePick(bucket: String, fileId: Long) {
        _uiState.update { it.copy(replacePending = OssReplacePending(bucket, fileId)) }
        sendEffect(OssDemoEffect.OpenReplaceImagePicker)
    }

    private fun replaceImagePicked(file: MultipartPartPayload?) {
        val pending = _uiState.value.replacePending ?: return
        _uiState.update { it.copy(replacePending = null) }
        if (file == null) return
        viewModelScope.launch {
            try {
                val res = AppContainer.ossManager.updateFileContent(pending.fileId.toString(), file)
                if (res.updated) invalidateBucket(pending.bucket)
                sendEffect(OssDemoEffect.ShowToast(if (res.updated) "已更换图片" else res.message.ifBlank { "更换失败" }))
            } catch (e: Exception) {
                sendEffect(OssDemoEffect.ShowToast(e.message ?: "更换失败"))
            }
        }
    }

    private fun deleteFile(bucket: String, fileId: Long) {
        viewModelScope.launch {
            try {
                val uid = currentUserId()
                AppContainer.ossManager.batchDelete(listOf(fileId), uid, bucket)
                invalidateBucket(bucket)
                sendEffect(OssDemoEffect.ShowToast("已删除"))
            } catch (e: Exception) {
                sendEffect(OssDemoEffect.ShowToast(e.message ?: "删除失败"))
            }
        }
    }

    private suspend fun currentUserId(): Long {
        val u = AppContainer.userManager.getCurrentUser() ?: error("未登录")
        if (u.userId <= 0L) error("无效用户")
        return u.userId
    }
}
