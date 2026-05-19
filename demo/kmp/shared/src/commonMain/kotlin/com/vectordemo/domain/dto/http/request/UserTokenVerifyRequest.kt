package com.vectordemo.domain.dto.http.request

import kotlinx.serialization.Serializable

@Serializable
data class UserTokenVerifyRequest(
    val userId: String? = null,
    val accessToken: String? = null,
)
