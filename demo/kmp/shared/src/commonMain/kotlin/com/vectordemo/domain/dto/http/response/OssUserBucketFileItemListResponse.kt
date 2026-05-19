package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssUserBucketFileItemListResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val items: List<OssUserBucketFileItemRow>? = null,
)
