package com.vectordemo.domain.dto.http.response

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

data class OssUserDeleteAllResponse(
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    val userId: String? = null,
    val totalCount: Int? = null,
    val successCount: Int? = null,
    val failCount: Int? = null,
    val message: String? = null
)
