package com.magicvector.domain.model.user

import com.magicvector.utils.json.GsonBean

data class UserSessionModel(
    val userId: Long,
    val account: String,
    val name: String,
    val avatarUrl: String,
    val accessToken: String,
    val password: String = "",
    val isCurrent: Boolean = false,
    val lastLoginAt: Long = 0L
): GsonBean {
    fun isEmpty(): Boolean {
        return userId <= 0L &&
                account.isEmpty()
    }
}