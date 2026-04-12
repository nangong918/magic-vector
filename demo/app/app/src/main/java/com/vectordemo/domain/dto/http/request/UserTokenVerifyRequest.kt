package com.vectordemo.domain.dto.http.request

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

data class UserTokenVerifyRequest(
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    var userId: String? = null,
    var accessToken: String? = null
)
