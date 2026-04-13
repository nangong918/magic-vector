package com.vectordemo.domain.dto.http.request

/** [userId] is decimal string in JSON (same as Spring [UserPasswordUpdateRequest]). */
data class UserPasswordUpdateRequest(
    var userId: String? = null,
    var oldPassword: String? = null,
    var newPassword: String? = null
)
