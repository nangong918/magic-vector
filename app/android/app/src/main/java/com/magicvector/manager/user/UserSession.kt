package com.magicvector.manager.user

data class UserSession(
    val userId: String,
    val account: String,
    val name: String,
    val avatarUrl: String,
    val accessToken: String
)
