package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class OssBatchDeleteResponse(
    val fileIdList: List<String>? = null,
    val successCount: Int? = null,
    val failCount: Int? = null,
    val message: String? = null,
)
