package com.vectordemo.domain.dto.http.response

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val code: String? = null,
    val message: String? = null,
    val data: T? = null,
)
