package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssFileContentUpdateResponse(
    val fileId: String? = null,
    val originFileName: String? = null,
    val url: String? = null,
    val updated: Boolean? = null,
    val message: String? = null,
)
