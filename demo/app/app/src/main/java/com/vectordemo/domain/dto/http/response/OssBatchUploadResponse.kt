package com.vectordemo.domain.dto.http.response

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

data class OssBatchUploadResponse(
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    val userId: String? = null,
    val bucketName: String? = null,
    val successCount: Int? = null,
    val failCount: Int? = null,
    val items: List<OssUploadItemResult>? = null
)
