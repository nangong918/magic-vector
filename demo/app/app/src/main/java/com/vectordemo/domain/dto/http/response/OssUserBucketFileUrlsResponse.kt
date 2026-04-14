package com.vectordemo.domain.dto.http.response

data class OssUserBucketFileUrlsResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val urlList: List<String>? = null
)
