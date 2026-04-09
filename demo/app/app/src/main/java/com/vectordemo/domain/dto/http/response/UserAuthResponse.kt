package com.vectordemo.domain.dto.http.response

data class UserAuthResponse(
    var userId: Long? = null,
    var account: String? = null,
    var name: String? = null,
    var avatarUrl: String? = null,
    var accessToken: String? = null
)
