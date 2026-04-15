package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssBucketFileIdListModel(
    val userId: Long,
    val bucketName: String,
    val fileIds: List<Long>
) : GsonBean
