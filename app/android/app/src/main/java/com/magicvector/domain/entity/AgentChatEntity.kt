package com.magicvector.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.data.domain.constant.chat.RoleTypeEnum

/**
 * Agent Item 业务持久化
 * @see com.magicvector.domain.model.agent.AgentChatModel
 */
@Entity(
    tableName = "agent_chat",
    indices = [
        // 核心索引1：覆盖「user_id + last_chat_time」（匹配queryFull/queryPage）
        Index(
            value = ["user_id", "last_chat_time"],
            name = "idx_user_last_chat_time"
        ),
        // 核心索引2：覆盖「user_id + agent_id」（匹配deleteByAgentId + 部分queryPage）
        Index(
            value = ["user_id", "agent_id"],
            name = "idx_user_agent"
        ),
        // 核心索引3：覆盖「user_id + name」（匹配queryFullByName/queryPageByName）
        Index(
            value = ["user_id", "name"],
            name = "idx_user_name"
        )
    ]
)
data class AgentChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,   // 前端内部的agentId

    // AgentChatModel
    @ColumnInfo(name = "agent_id")  // SpringBoot后端分配的agentId
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