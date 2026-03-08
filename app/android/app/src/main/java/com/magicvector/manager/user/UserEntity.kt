package com.magicvector.manager.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_key")
    val sessionKey: String = CURRENT_SESSION_KEY,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "account")
    val account: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String,
    @ColumnInfo(name = "access_token")
    val accessToken: String
) {
    companion object {
        const val CURRENT_SESSION_KEY = "current"
    }
}
