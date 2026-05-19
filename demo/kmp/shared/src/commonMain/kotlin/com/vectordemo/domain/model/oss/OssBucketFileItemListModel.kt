package com.vectordemo.domain.model.oss

data class OssBucketFileItemListModel(
    val userId: Long,
    val bucketName: String,
    val items: List<OssBucketFileItemModel>,
)
