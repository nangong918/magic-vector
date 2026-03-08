package com.magicvector.manager.user

data class UserSession(
    val userId: Long,
    val account: String,
    val name: String,
    val avatarUrl: String,
    val accessToken: String
)
