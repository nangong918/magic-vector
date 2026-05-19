package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssBatchUploadResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val successCount: Int? = null,
    val failCount: Int? = null,
    val items: List<OssUploadItemResult>? = null,
)
