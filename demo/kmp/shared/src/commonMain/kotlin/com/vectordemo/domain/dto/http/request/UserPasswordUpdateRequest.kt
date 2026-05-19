package com.vectordemo.domain.dto.http.request

import kotlinx.serialization.Serializable

@Serializable
data class UserPasswordUpdateRequest(
    val userId: String? = null,
    val oldPassword: String? = null,
    val newPassword: String? = null,
)
