package com.magicvector.manager.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long = CURRENT_ROW_ID,
    @ColumnInfo(name = "user_id")
    val userId: Long,
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
        const val CURRENT_ROW_ID = 1L
    }
}
