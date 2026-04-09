package com.vectordemo.domain.dto.http.request

data class UserLoginRequest(
    var account: String = "",
    var password: String = ""
)
