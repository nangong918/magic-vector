package com.magicvector.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.data.domain.constant.chat.RoleTypeEnum

/**
 * Agent Item 业务持久化
 * @see com.magicvector.domain.model.agent.AgentChatModel
 */
@Entity(tableName = "agent_chat")
data class AgentChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    // AgentChatModel
    @ColumnInfo(name = "agent_id")
    val agentId: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "last_chat_time")
    val lastChatTime: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    // AgentChatVo
    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,
    // AgentVo
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    // ChatMessageVo
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "chat_time")
    val chatTime: String,
    @ColumnInfo(name = "role")
    val role: Int = RoleTypeEnum.AGENT.value,
)