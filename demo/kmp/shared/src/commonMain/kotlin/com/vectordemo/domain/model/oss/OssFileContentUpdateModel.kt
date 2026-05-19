package com.vectordemo.domain.model.oss

data class OssFileContentUpdateModel(
    val fileId: String,
    val originFileName: String,
    val url: String,
    val updated: Boolean,
    val message: String,
)
