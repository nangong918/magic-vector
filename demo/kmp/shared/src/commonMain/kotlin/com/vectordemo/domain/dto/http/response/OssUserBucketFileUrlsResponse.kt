package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssUserBucketFileUrlsResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val urlList: List<String>? = null,
)
