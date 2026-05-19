package com.vectordemo.ui.oss

import com.vectordemo.domain.model.oss.OssPickedImage

data class OssMediaActions(
    val pickMainImage: (onResult: (OssPickedImage?) -> Unit) -> Unit = { it(null) },
    val pickReplaceImage: (onResult: (OssPickedImage?) -> Unit) -> Unit = { it(null) },
    val downloadToGallery: suspend (url: String, displayName: String) -> String = { _, _ ->
        error("当前平台尚未接入相册下载")
    },
)

object OssMediaBridge {
    private var actions: OssMediaActions = OssMediaActions()

    fun setActions(value: OssMediaActions) {
        actions = value
    }

    fun pickMainImage(onResult: (OssPickedImage?) -> Unit) {
        actions.pickMainImage(onResult)
    }

    fun pickReplaceImage(onResult: (OssPickedImage?) -> Unit) {
        actions.pickReplaceImage(onResult)
    }

    suspend fun downloadToGallery(url: String, displayName: String): String =
        actions.downloadToGallery(url, displayName)
}
