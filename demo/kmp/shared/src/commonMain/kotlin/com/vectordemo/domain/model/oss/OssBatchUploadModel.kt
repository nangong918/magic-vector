package com.vectordemo.domain.model.oss

import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.dto.http.response.OssBatchUploadResponse
import com.vectordemo.domain.dto.http.response.OssUploadItemResult
import com.vectordemo.domain.dto.http.request.appendPayload
import io.ktor.client.request.forms.FormBuilder

data class OssBatchUploadModel(
    val userId: Long,
    val bucketName: String,
    val successCount: Int,
    val failCount: Int,
    val items: List<OssUploadItemModel>,
) {
    companion object {
        fun multipartFormData(
            userId: String,
            bucketName: String?,
            files: List<MultipartPartPayload>,
        ): FormBuilder.() -> Unit = {
            append("userId", userId)
            if (!bucketName.isNullOrBlank()) append("bucketName", bucketName)
            files.forEach { file ->
                appendPayload(file, formFieldName = "files")
            }
        }

        fun fromResponse(response: OssBatchUploadResponse): OssBatchUploadModel {
            val uid = response.userId?.toLongOrNull() ?: 0L
            val items = response.items.orEmpty().map { itemToModel(it) }
            return OssBatchUploadModel(
                userId = uid,
                bucketName = response.bucketName.orEmpty(),
                successCount = response.successCount ?: 0,
                failCount = response.failCount ?: 0,
                items = items,
            )
        }

        private fun itemToModel(it: OssUploadItemResult): OssUploadItemModel = OssUploadItemModel(
            originFileName = it.originFileName.orEmpty(),
            success = it.success,
            duplicated = it.duplicated,
            fileId = it.fileId.orEmpty(),
            url = it.url.orEmpty(),
            message = it.message.orEmpty(),
        )
    }
}
