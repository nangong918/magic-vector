package com.vectordemo.domain.model.oss

import com.vectordemo.utils.json.GsonBean

data class OssBatchDeleteModel(
    val fileIds: List<Long>,
    val successCount: Int,
    val failCount: Int,
    val message: String
) : GsonBean
