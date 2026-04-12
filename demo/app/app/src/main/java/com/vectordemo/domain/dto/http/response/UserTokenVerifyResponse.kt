package com.vectordemo.domain.dto.http.response

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

data class UserTokenVerifyResponse(
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    var userId: String? = null,
    var valid: Boolean? = null,
    var message: String? = null
)
