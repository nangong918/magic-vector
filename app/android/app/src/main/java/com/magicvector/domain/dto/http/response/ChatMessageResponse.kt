package com.magicvector.domain.dto.http.response

import com.data.domain.constant.chat.MessageTypeEnum
import com.data.domain.constant.chat.RoleTypeEnum

class ChatMessageListResponse {
    val messageList: List<ChatMessageDto> = emptyList()
}

// 后端返回的数据类型
data class ChatMessageDto(
    // ========== ChatMessageModel ==========
    var id: Long = 0L,
    var agentId: Long = 0L,
    var userId: Long = 0L,
    var messageId: Long = 0L,
    var timestamp: Long = 0L,

    // ========== ChatMessageVo ==========
    var imgUrl: String = "",
    var messageType: Int = MessageTypeEnum.TEXT.value,

    // ========== ChatBriefMessageVo ==========
    val content: String = "",
    val chatTime: String = "",
    val role: Int = RoleTypeEnum.AGENT.value
)