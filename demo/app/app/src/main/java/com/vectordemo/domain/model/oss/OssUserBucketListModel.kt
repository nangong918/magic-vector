package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssUserBucketListModel(
    val userId: Long,
    val bucketNames: List<String>
) : GsonBean
