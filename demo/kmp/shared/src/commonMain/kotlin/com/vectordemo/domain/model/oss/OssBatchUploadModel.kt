package com.vectordemo.domain.model.oss

data class OssBatchUploadModel(
    val userId: Long,
    val bucketName: String,
    val successCount: Int,
    val failCount: Int,
    val items: List<OssUploadItemModel>,
)
