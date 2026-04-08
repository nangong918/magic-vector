package com.magicvector.domain.dto.http.response

import com.magicvector.domain.constant.chat.MessageTypeEnum
import com.magicvector.domain.constant.chat.RoleTypeEnum

class ChatMessageListResponse {
    val messageList: List<ChatMessageDto> = emptyList()
}

// 后端返回的数据类型
data class ChatMessageDto(
    // ========== ChatMessageModel ==========
    var id: String = "0",
    var agentId: String = "0",
    var userId: String = "0",
    var messageId: String = "0",
    var timestamp: String = "0",

    // ========== ChatMessageVo ==========
    var imgUrl: String = "",
    var messageType: Int = MessageTypeEnum.TEXT.value,

    // ========== ChatBriefMessageVo ==========
    val content: String = "",
    val chatTime: String = "",
    val role: Int = RoleTypeEnum.AGENT.value
)