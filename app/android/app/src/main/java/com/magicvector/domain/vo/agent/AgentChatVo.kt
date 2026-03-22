package com.magicvector.domain.vo.agent

import com.magicvector.domain.vo.message.ChatBriefMessageVo

/**
 * Agent Item 包含聊天 vo
 */
data class AgentChatVo(
    val agentVo: AgentVo,
    val chatBriefMessageVo: ChatBriefMessageVo,
    val unreadCount: Int = 0
)
