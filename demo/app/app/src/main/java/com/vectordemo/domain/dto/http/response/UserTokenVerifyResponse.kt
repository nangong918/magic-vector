package com.vectordemo.domain.dto.http.response

data class UserTokenVerifyResponse(
    var userId: Long? = null,
    var valid: Boolean? = null,
    var message: String? = null
)
