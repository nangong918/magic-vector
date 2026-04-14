package com.vectordemo.viewModel.oss

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vectordemo.MainApplication
import com.vectordemo.dataSource.remote.RemoteApiSource
import com.vectordemo.utils.media.GalleryImageDownloader
import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class OssDemoUiState(
    val touristBlocked: Boolean = false,
    val toast: String? = null,
    val buckets: List<String> = emptyList(),
    val expandedBuckets: Set<String> = emptySet(),
    val filesByBucket: Map<String, List<OssUserBucketFileItemRow>> = emptyMap(),
    val loadingBuckets: Boolean = false,
    val loadingBucket: String? = null
)

class OssDemoViewModel(application: Application) : AndroidViewModel(application) {

    private val remote: RemoteApiSource = MainApplication.getRemoteApiSource()

    private val _uiState = MutableStateFlow(OssDemoUiState())
    val uiState: StateFlow<OssDemoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val u = MainApplication.getUserManager().getCurrentUser()
            val blocked = u == null || u.userId <= 0L || u.userId == 1L ||
                u.accessToken.isBlank() || u.accessToken == "tourist" || u.account == "tourist"
            _uiState.update { it.copy(touristBlocked = blocked) }
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toast = null) }
    }

    fun saveImageToGallery(url: String, rawDisplayName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    GalleryImageDownloader.downloadToGallery(
                        getApplication(),
                        url,
                        rawDisplayName
                    )
                }
                _uiState.update { it.copy(toast = "已保存到相册（Pictures/VectorDemo）") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toast = e.message ?: "保存到相册失败") }
            }
        }
    }

    private suspend fun currentUserIdStr(): String {
        val u = MainApplication.getUserManager().getCurrentUser()
            ?: throw IllegalStateException("未登录")
        if (u.userId <= 0L) throw IllegalStateException("无效用户")
        return u.userId.toString()
    }

    fun refreshBuckets() {
        viewModelScope.launch {
            if (_uiState.value.touristBlocked) return@launch
            val expandedBefore = _uiState.value.expandedBuckets.toSet()
            try {
                _uiState.update {
                    it.copy(
                        loadingBuckets = true,
                        filesByBucket = emptyMap(),
                        loadingBucket = null
                    )
                }
                val uid = currentUserIdStr()
                val res = remote.ossUserBucketList(uid)
                val newBuckets = res.bucketNameList.orEmpty()
                _uiState.update {
                    it.copy(
                        loadingBuckets = false,
                        buckets = newBuckets
                    )
                }
                expandedBefore.intersect(newBuckets.toSet()).forEach { b ->
                    loadBucketFiles(b, force = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loadingBuckets = false, toast = e.message ?: "加载存储桶失败")
                }
            }
        }
    }

    fun toggleBucket(bucket: String) {
        val expanded = _uiState.value.expandedBuckets.toMutableSet()
        if (!expanded.add(bucket)) {
            expanded.remove(bucket)
        }
        _uiState.update { it.copy(expandedBuckets = expanded) }
        if (expanded.contains(bucket)) {
            loadBucketFiles(bucket)
        }
    }

    fun loadBucketFiles(bucket: String, force: Boolean = false) {
        if (!force && _uiState.value.filesByBucket.containsKey(bucket)) return
        viewModelScope.launch {
            if (_uiState.value.touristBlocked) return@launch
            try {
                _uiState.update { it.copy(loadingBucket = bucket) }
                val uid = currentUserIdStr()
                val res = remote.ossUserBucketFileItemList(uid, bucket)
                val items = res.items.orEmpty()
                _uiState.update {
                    val map = it.filesByBucket.toMutableMap()
                    map[bucket] = items
                    it.copy(loadingBucket = null, filesByBucket = map)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loadingBucket = null, toast = e.message ?: "加载文件失败")
                }
            }
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

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            if (_uiState.value.touristBlocked) return@launch
            try {
                val ctx = getApplication<Application>()
                val cr = ctx.contentResolver
                val mime = cr.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    cr.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取图片")
                }
                val uid = currentUserIdStr()
                val dir = File(ctx.cacheDir, "oss_upload")
                dir.mkdirs()
                val ext = when {
                    mime.contains("png", ignoreCase = true) -> "png"
                    mime.contains("webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }
                val tmp = File(dir, "up_${System.currentTimeMillis()}.$ext")
                tmp.writeBytes(bytes)
                val res = remote.ossBatchUploadSingle(uid, null, tmp, tmp.name, mime)
                val ok = res.items?.any { it.success } == true
                _uiState.update {
                    it.copy(
                        toast = if (ok) "上传成功" else (res.items?.firstOrNull()?.message ?: "上传失败")
                    )
                }
                tmp.delete()
            } catch (e: Exception) {
                _uiState.update { it.copy(toast = e.message ?: "上传失败") }
            }
        }
    }

    fun replaceFile(fileId: String, bucket: String, uri: Uri) {
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
                val res = remote.ossUpdateFileContent(fileId, body, "upload.$ext")
                val ok = res.updated == true
                if (ok) {
                    invalidateBucket(bucket)
                }
                _uiState.update {
                    it.copy(toast = if (ok) "已更换图片" else (res.message ?: "更换失败"))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(toast = e.message ?: "更换失败") }
            }
        }
    }

    fun deleteFile(fileId: String, bucket: String) {
        viewModelScope.launch {
            try {
                remote.ossBatchDelete(listOf(fileId))
                invalidateBucket(bucket)
                _uiState.update { it.copy(toast = "已删除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toast = e.message ?: "删除失败") }
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OssDemoViewModel(MainApplication.getApp()) as T
            }
        }
    }
}
