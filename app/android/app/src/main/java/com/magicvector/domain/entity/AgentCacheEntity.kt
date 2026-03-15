package com.magicvector.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_cache")
data class AgentCacheEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "agent_id")
    val agentId: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val name: String,
    val description: String,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)