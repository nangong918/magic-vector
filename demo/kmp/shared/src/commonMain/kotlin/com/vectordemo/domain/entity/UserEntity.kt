package com.vectordemo.domain.entity

data class UserEntity(
    val id: Long = 0L,
    val userId: Long,
    val account: String,
    val name: String,
    val avatarUrl: String,
    val accessToken: String,
    val password: String,
    val isCurrent: Boolean,
    val lastLoginAt: Long,
)
