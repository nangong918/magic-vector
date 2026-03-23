package com.magicvector.domain.vo.message

import com.data.domain.constant.chat.RoleTypeEnum

/**
 * 聊天简略消息vo
 * 详情vo:
 * @see ChatMessageVO
 */
data class ChatBriefMessageVO(
    // 内容
    val content: String,
    // 时间(展示用)
    val chatTime: String,
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    val role: Int = RoleTypeEnum.AGENT.value
)
