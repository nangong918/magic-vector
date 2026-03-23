package com.magicvector.domain.vo.agent

import com.magicvector.domain.vo.message.ChatBriefMessageVO

/**
 * Agent Item 包含聊天 vo
 */
data class AgentChatVO(
    val agentVo: AgentVO,
    val chatBriefMessageVo: ChatBriefMessageVO,
    val unreadCount: Int = 0
)
