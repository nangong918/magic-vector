package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssBucketFileItemModel(
    val fileId: Long,
    val originFileName: String,
    val url: String
) : GsonBean
