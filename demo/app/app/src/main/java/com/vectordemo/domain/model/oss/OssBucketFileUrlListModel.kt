package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssBucketFileUrlListModel(
    val userId: Long,
    val bucketName: String,
    val urls: List<String>
) : GsonBean
