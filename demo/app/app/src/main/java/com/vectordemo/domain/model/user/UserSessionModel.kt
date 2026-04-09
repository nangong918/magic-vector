package com.vectordemo.domain.model.user

import com.vectordemo.utils.json.GsonBean

data class UserSessionModel(
    val userId: Long,
    val account: String,
    val name: String,
    val avatarUrl: String,
    val accessToken: String,
    val password: String = "",
    val isCurrent: Boolean = false,
    val lastLoginAt: Long = 0L
) : GsonBean
