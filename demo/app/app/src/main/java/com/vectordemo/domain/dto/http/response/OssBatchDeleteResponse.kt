package com.vectordemo.domain.dto.http.response

/** [fileIdList] elements are decimal strings in JSON. */
data class OssBatchDeleteResponse(
    val fileIdList: List<String>? = null,
    val successCount: Int? = null,
    val failCount: Int? = null,
    val message: String? = null
)
