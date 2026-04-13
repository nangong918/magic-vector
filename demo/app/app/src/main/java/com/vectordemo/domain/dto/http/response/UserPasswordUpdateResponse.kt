package com.vectordemo.domain.dto.http.response

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

data class UserPasswordUpdateResponse(
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    val userId: String? = null,
    val updated: Boolean? = null,
    val message: String? = null
)
