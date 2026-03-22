package com.magicvector.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.data.domain.constant.chat.MessageTypeEnum
import com.data.domain.constant.chat.RoleTypeEnum

/**
 * 聊天消息业务持久化
 * @see com.magicvector.domain.model.chat.ChatMessageModel
 */
@Entity(
    tableName = "chat_message",
    indices = [
        Index(value = ["agent_id", "timestamp", "id"], name = "idx_agent_time_id"),
        Index(value = ["user_id", "agent_id"], name = "idx_user_agent")
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,

    // ========== ChatMessageModel ==========

    @ColumnInfo(name = "agent_id")
    var agentId: Long,
    @ColumnInfo(name = "user_id")
    var userId: Long,
    @ColumnInfo(name = "message_id")
    var messageId: Long,
    @ColumnInfo(name = "timestamp")
    var timestamp: Long,

    // ========== ChatMessageVo ==========

    @ColumnInfo(name = "img_url")
    var imgUrl: String,
    @ColumnInfo(name = "message_type")
    var messageType: Int = MessageTypeEnum.TEXT.value,

    // ========== ChatBriefMessageVo ==========

    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "chat_time")
    val chatTime: String,
    @ColumnInfo(name = "role")
    val role: Int = RoleTypeEnum.AGENT.value
)