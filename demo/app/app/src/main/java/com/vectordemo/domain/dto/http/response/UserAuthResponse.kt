package com.vectordemo.domain.dto.http.response

import com.google.gson.annotations.JsonAdapter
import com.vectordemo.utils.json.WireUserIdJsonDeserializer

/** [userId] is decimal string on the wire (JSON); parse to Long for Room/session. */
data class UserAuthResponse(
    @JsonAdapter(WireUserIdJsonDeserializer::class)
    var userId: String? = null,
    var account: String? = null,
    var name: String? = null,
    var avatarUrl: String? = null,
    var accessToken: String? = null
)
