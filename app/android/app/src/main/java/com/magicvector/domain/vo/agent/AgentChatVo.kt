package com.magicvector.domain.vo.agent

import com.magicvector.domain.vo.message.ChatMessageVo

/**
 * Agent Item 包含聊天 vo
 */
data class AgentChatVo(
    val agentVo: AgentVo,
    val chatMessageVo: ChatMessageVo,
    val unreadCount: Int = 0
)
