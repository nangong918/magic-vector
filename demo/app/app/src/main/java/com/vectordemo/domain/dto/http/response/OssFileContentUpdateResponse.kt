package com.vectordemo.domain.dto.http.response

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

data class OssFileContentUpdateResponse(
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    val fileId: String? = null,
    val originFileName: String? = null,
    val url: String? = null,
    val updated: Boolean? = null,
    val message: String? = null
)
