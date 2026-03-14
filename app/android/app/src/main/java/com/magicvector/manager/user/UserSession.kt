package com.magicvector.manager.user

import com.core.baseutil.json.GsonBean

data class UserSession(
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
