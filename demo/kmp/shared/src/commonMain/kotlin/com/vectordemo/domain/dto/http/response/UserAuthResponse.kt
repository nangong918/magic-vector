package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class UserAuthResponse(
    val userId: String? = null,
    val account: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val accessToken: String? = null,
)
