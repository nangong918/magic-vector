package com.vectordemo.domain.model.oss

data class OssUserBucketListModel(
    val userId: Long,
    val bucketNames: List<String>,
)
