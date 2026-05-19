package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssUserBucketFileItemRow(
    val fileId: String? = null,
    val originFileName: String? = null,
    val url: String? = null,
)
