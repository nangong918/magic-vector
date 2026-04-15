package com.vectordemo.viewModel.oss

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vectordemo.MainApplication
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.manager.oss.OssManager
import com.vectordemo.utils.media.GalleryImageDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class OssReplacePending(val bucket: String, val fileId: Long)

data class OssDemoState(
    val touristBlocked: Boolean = false,
    val buckets: List<String> = emptyList(),
    val expandedBuckets: Set<String> = emptySet(),
    val filesByBucket: Map<String, List<OssBucketFileItemModel>> = emptyMap(),
    val loadingBuckets: Boolean = false,
    val loadingBucket: String? = null,
    val pickedUploadUri: Uri? = null,
    val replacePending: OssReplacePending? = null
)

sealed class OssDemoIntent {
    data object RefreshBuckets : OssDemoIntent()
    data class ToggleBucket(val bucket: String) : OssDemoIntent()
    data class LoadBucketFiles(val bucket: String, val force: Boolean = false) : OssDemoIntent()
    data object RequestMainImagePick : OssDemoIntent()
    data class MainImagePicked(val uri: Uri?) : OssDemoIntent()
    data object UploadSubmit : OssDemoIntent()
    data class DownloadImage(val url: String, val displayName: String) : OssDemoIntent()
    data class RequestReplacePick(val bucket: String, val fileId: Long) : OssDemoIntent()
    data class ReplaceImagePicked(val uri: Uri?) : OssDemoIntent()
    data class DeleteFile(val bucket: String, val fileId: Long) : OssDemoIntent()
}

sealed class OssDemoEffect {
    data class ShowToast(val message: String) : OssDemoEffect()
    data object OpenMainImagePicker : OssDemoEffect()
    data object OpenReplaceImagePicker : OssDemoEffect()
}

class OssDemoVm(application: Application) : AndroidViewModel(application) {

    private val oss: OssManager = MainApplication.getOssManager()

    private val _uiState = MutableStateFlow(OssDemoState())
    val uiState: StateFlow<OssDemoState> = _uiState.asStateFlow()

    private val _effect = Channel<OssDemoEffect>(Channel.BUFFERED)
    val effect: Flow<OssDemoEffect> = _effect.receiveAsFlow()

    init {
        initialize()
    }

    fun processIntent(intent: OssDemoIntent) {
        when (intent) {
            OssDemoIntent.RefreshBuckets -> refreshBuckets()
            is OssDemoIntent.ToggleBucket -> toggleBucket(intent.bucket)
            is OssDemoIntent.LoadBucketFiles -> loadBucketFiles(intent.bucket, intent.force)
            OssDemoIntent.RequestMainImagePick -> sendEffect(OssDemoEffect.OpenMainImagePicker)
            is OssDemoIntent.MainImagePicked -> _uiState.update { it.copy(pickedUploadUri = intent.uri) }
            OssDemoIntent.UploadSubmit -> uploadImage()
            is OssDemoIntent.DownloadImage -> saveImageToGallery(intent.url, intent.displayName)
            is OssDemoIntent.RequestReplacePick -> requestReplacePick(intent.bucket, intent.fileId)
            is OssDemoIntent.ReplaceImagePicked -> replaceImagePicked(intent.uri)
            is OssDemoIntent.DeleteFile -> deleteFile(intent.bucket, intent.fileId)
        }
    }

    private fun sendEffect(effect: OssDemoEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun initialize() {
        viewModelScope.launch {
            val u = MainApplication.getUserManager().getCurrentUser()
            val blocked = u == null || u.userId <= 0L || u.userId == 1L ||
                u.accessToken.isBlank() || u.accessToken == "tourist" || u.account == "tourist"
            _uiState.update { it.copy(touristBlocked = blocked) }
            if (!blocked) {
                doRefreshBuckets()
            }
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
            _uiState.update {
                it.copy(
                    loadingBuckets = true,
                    filesByBucket = emptyMap(),
                    loadingBucket = null
                )
            }
            val uid = currentUserId()
            val res = oss.syncUserBucketList(uid)
            val newBuckets = res.bucketNames
            _uiState.update {
                it.copy(
                    loadingBuckets = false,
                    buckets = newBuckets
                )
            }
            expandedBefore.intersect(newBuckets.toSet()).forEach { b ->
                doLoadBucketFiles(b, force = true)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(loadingBuckets = false) }
            sendEffect(OssDemoEffect.ShowToast(e.message ?: "加载存储桶失败"))
        }
    }

    private fun toggleBucket(bucket: String) {
        val expanded = _uiState.value.expandedBuckets.toMutableSet()
        if (!expanded.add(bucket)) {
            expanded.remove(bucket)
        }
        _uiState.update { it.copy(expandedBuckets = expanded) }
        if (expanded.contains(bucket)) {
            loadBucketFiles(bucket, force = false)
        }
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
            val res = oss.syncBucketFileItemList(uid, bucket)
            val items = res.items
            _uiState.update {
                val map = it.filesByBucket.toMutableMap()
                map[bucket] = items
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
        if (_uiState.value.expandedBuckets.contains(bucket)) {
            loadBucketFiles(bucket, force = true)
        }
    }

    private fun uploadImage() {
        viewModelScope.launch {
            if (_uiState.value.touristBlocked) return@launch
            val uri = _uiState.value.pickedUploadUri
            if (uri == null) {
                sendEffect(OssDemoEffect.ShowToast("请先选择图片"))
                return@launch
            }
            try {
                val ctx = getApplication<Application>()
                val cr = ctx.contentResolver
                val mime = cr.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    cr.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取图片")
                }
                val uid = currentUserId()
                val dir = File(ctx.cacheDir, "oss_upload")
                dir.mkdirs()
                val ext = when {
                    mime.contains("png", ignoreCase = true) -> "png"
                    mime.contains("webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }
                val tmp = File(dir, "up_${System.currentTimeMillis()}.$ext")
                tmp.writeBytes(bytes)
                val res = oss.batchUploadSingle(uid, null, tmp, tmp.name, mime)
                val ok = res.items.any { it.success }
                sendEffect(
                    OssDemoEffect.ShowToast(
                        if (ok) {
                            "上传成功"
                        } else {
                            res.items.firstOrNull()?.message?.takeIf { m -> m.isNotBlank() } ?: "上传失败"
                        }
                    )
                )
                tmp.delete()
            } catch (e: Exception) {
                sendEffect(OssDemoEffect.ShowToast(e.message ?: "上传失败"))
            }
        }
    }

    private fun saveImageToGallery(url: String, rawDisplayName: String) {
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    GalleryImageDownloader.downloadToGallery(
                        getApplication(),
                        url,
                        rawDisplayName
                    )
                }
                sendEffect(OssDemoEffect.ShowToast("下载成功\n$path"))
            } catch (e: Exception) {
                sendEffect(OssDemoEffect.ShowToast(e.message ?: "保存到相册失败"))
            }
        }
    }

    private fun requestReplacePick(bucket: String, fileId: Long) {
        _uiState.update { it.copy(replacePending = OssReplacePending(bucket, fileId)) }
        sendEffect(OssDemoEffect.OpenReplaceImagePicker)
    }

    private fun replaceImagePicked(uri: Uri?) {
        val pending = _uiState.value.replacePending ?: return
        _uiState.update { it.copy(replacePending = null) }
        if (uri == null) return
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val cr = ctx.contentResolver
                val mime = cr.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    cr.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取图片")
                }
                val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
                val ext = when {
                    mime.contains("png", ignoreCase = true) -> "png"
                    mime.contains("webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }
                val res = oss.updateFileContent(pending.fileId.toString(), body, "upload.$ext")
                val ok = res.updated
                if (ok) {
                    invalidateBucket(pending.bucket)
                }
                sendEffect(
                    OssDemoEffect.ShowToast(
                        if (ok) "已更换图片" else res.message.ifBlank { "更换失败" }
                    )
                )
            } catch (e: Exception) {
                sendEffect(OssDemoEffect.ShowToast(e.message ?: "更换失败"))
            }
        }
    }

    private fun deleteFile(bucket: String, fileId: Long) {
        viewModelScope.launch {
            try {
                val uid = currentUserId()
                oss.batchDelete(listOf(fileId), uid, bucket)
                invalidateBucket(bucket)
                sendEffect(OssDemoEffect.ShowToast("已删除"))
            } catch (e: Exception) {
                sendEffect(OssDemoEffect.ShowToast(e.message ?: "删除失败"))
            }
        }
    }

    private suspend fun currentUserId(): Long {
        val u = MainApplication.getUserManager().getCurrentUser()
            ?: throw IllegalStateException("未登录")
        if (u.userId <= 0L) throw IllegalStateException("无效用户")
        return u.userId
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OssDemoVm(MainApplication.getApp()) as T
            }
        }
    }
}
