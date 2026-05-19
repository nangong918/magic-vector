package com.vectordemo.domain.model.oss

data class OssUploadItemModel(
    val originFileName: String,
    val success: Boolean,
    val duplicated: Boolean,
    val fileId: String,
    val url: String,
    val message: String,
)
