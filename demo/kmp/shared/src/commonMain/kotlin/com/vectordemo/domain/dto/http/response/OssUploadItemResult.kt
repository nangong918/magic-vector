package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssUploadItemResult(
    val originFileName: String? = null,
    val success: Boolean = false,
    val duplicated: Boolean = false,
    val fileId: String? = null,
    val url: String? = null,
    val message: String? = null,
)
