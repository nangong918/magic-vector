package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class UserTokenVerifyResponse(
    val userId: String? = null,
    val valid: Boolean? = null,
    val message: String? = null,
)
