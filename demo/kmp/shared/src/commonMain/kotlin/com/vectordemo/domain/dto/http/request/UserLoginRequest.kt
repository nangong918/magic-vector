package com.vectordemo.domain.dto.http.request

import kotlinx.serialization.Serializable

@Serializable
data class UserLoginRequest(
    val account: String = "",
    val password: String = "",
)
