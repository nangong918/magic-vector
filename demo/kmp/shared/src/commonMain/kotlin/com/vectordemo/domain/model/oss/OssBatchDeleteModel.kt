package com.vectordemo.domain.model.oss

data class OssBatchDeleteModel(
    val fileIds: List<Long>,
    val successCount: Int,
    val failCount: Int,
    val message: String,
)
