package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssBucketFileItemListModel(
    val userId: Long,
    val bucketName: String,
    val items: List<OssBucketFileItemModel>
) : GsonBean
