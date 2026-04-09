package com.vectordemo.domain.dto.http.request

data class UserTokenVerifyRequest(
    var userId: Long? = null,
    var accessToken: String? = null
)
