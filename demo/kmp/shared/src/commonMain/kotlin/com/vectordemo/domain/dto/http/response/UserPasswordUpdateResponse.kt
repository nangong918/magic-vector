package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class UserPasswordUpdateResponse(
    val userId: String? = null,
    val updated: Boolean? = null,
    val message: String? = null,
)
