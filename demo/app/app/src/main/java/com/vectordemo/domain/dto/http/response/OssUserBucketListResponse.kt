package com.vectordemo.domain.dto.http.response

data class OssUserBucketListResponse(
    val userId: String? = null,
    val bucketNameList: List<String>? = null
)
