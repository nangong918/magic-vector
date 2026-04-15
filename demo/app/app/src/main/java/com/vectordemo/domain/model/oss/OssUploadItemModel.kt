package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssUploadItemModel(
    val originFileName: String,
    val success: Boolean,
    val duplicated: Boolean,
    val fileId: String,
    val url: String,
    val message: String
) : GsonBean
