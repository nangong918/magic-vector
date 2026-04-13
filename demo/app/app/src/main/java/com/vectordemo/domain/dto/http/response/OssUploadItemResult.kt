package com.vectordemo.domain.dto.http.response

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

data class OssUploadItemResult(
    val originFileName: String? = null,
    val success: Boolean = false,
    val duplicated: Boolean = false,
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    val fileId: String? = null,
    val url: String? = null,
    val message: String? = null
)
