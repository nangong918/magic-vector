package com.magicvector.manager.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_message",
    indices = [
        Index(value = ["agent_id", "chat_timestamp", "id"], name = "idx_agent_time_id"),
        Index(value = ["user_id", "agent_id"], name = "idx_user_agent")
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "agent_id")
    val agentId: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val content: String,
    @ColumnInfo(name = "chat_timestamp")
    val chatTimestamp: Long,
    @ColumnInfo(name = "chat_time")
    val chatTime: String,
    val role: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
