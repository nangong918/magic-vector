package com.vectordemo.activity

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.model.oss.OssPickedImage
import com.vectordemo.ui.oss.OssMediaActions
import com.vectordemo.ui.oss.OssMediaBridge
import com.vectordemo.utils.media.GalleryImageDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class OssMediaRouter(private val activity: ComponentActivity) {
    private val appCtx: Context = activity.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var pendingMainCallback: ((OssPickedImage?) -> Unit)? = null
    private var pendingReplaceCallback: ((OssPickedImage?) -> Unit)? = null

    private val pickMainLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val callback = pendingMainCallback
        pendingMainCallback = null
        deliverPicked(callback, uri)
    }

    private val pickReplaceLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val callback = pendingReplaceCallback
        pendingReplaceCallback = null
        deliverPicked(callback, uri)
    }

    private fun deliverPicked(callback: ((OssPickedImage?) -> Unit)?, uri: Uri?) {
        if (callback == null) return
        if (uri == null) {
            callback(null)
            return
        }
        scope.launch {
            val picked = runCatching { buildPickedImage(uri) }.getOrNull()
            callback(picked)
        }
    }

    private suspend fun buildPickedImage(uri: Uri): OssPickedImage = withContext(Dispatchers.IO) {
        val cr = appCtx.contentResolver
        val mime = cr.getType(uri) ?: "image/jpeg"
        val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取图片")
        val ext = when {
            mime.contains("png", ignoreCase = true) -> "png"
            mime.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val fileName = "upload_${System.currentTimeMillis()}.$ext"
        OssPickedImage(
            previewModel = uri.toString(),
            uploadPayload = MultipartPartPayload(
                fieldName = "file",
                fileName = fileName,
                mimeType = mime,
                bytes = bytes,
            ),
        )
    }

    fun install() {
        OssMediaBridge.setActions(
            OssMediaActions(
                pickMainImage = { onResult ->
                    pendingMainCallback = onResult
                    pickMainLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                pickReplaceImage = { onResult ->
                    pendingReplaceCallback = onResult
                    pickReplaceLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                downloadToGallery = { url, displayName ->
                    withContext(Dispatchers.IO) {
                        GalleryImageDownloader.downloadToGallery(appCtx, url, displayName)
                    }
                },
            ),
        )
    }

    fun clear() {
        pendingMainCallback = null
        pendingReplaceCallback = null
        OssMediaBridge.setActions(OssMediaActions())
    }
}

private var ossMediaRouter: OssMediaRouter? = null

fun installOssMediaRouter(activity: ComponentActivity) {
    ossMediaRouter?.clear()
    ossMediaRouter = OssMediaRouter(activity).also { it.install() }
}

fun clearOssMediaRouter() {
    ossMediaRouter?.clear()
    ossMediaRouter = null
}
