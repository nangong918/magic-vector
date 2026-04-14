package com.vectordemo.domain.dto.http.response

data class OssUserBucketFileItemListResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val items: List<OssUserBucketFileItemRow>? = null
)
