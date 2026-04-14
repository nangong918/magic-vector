package com.vectordemo.domain.dto.http.response

data class OssUserBucketFileIdsResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val fileIdList: List<String>? = null
)
