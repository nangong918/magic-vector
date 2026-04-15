package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssBatchUploadModel(
    val userId: Long,
    val bucketName: String,
    val successCount: Int,
    val failCount: Int,
    val items: List<OssUploadItemModel>
) : GsonBean
