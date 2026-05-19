package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssUserBucketListResponse(
    val userId: String? = null,
    val bucketNameList: List<String>? = null,
)
