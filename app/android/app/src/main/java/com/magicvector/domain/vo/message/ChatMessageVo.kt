package com.magicvector.domain.vo.message

import com.data.domain.constant.chat.RoleTypeEnum

/**
 * 聊天消息vo
 */
data class ChatMessageVo(
    // 内容
    val content: String,
    // 时间(展示用)
    val chatTime: String,
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    val role: Int = RoleTypeEnum.AGENT.value
)
