package com.magicvector.domain.bo

import com.magicvector.domain.vo.agent.AgentVO
import java.io.Serializable

/**
 * MessageList打开Chat页面的传递Business Object数据结构
 */
data class AgentChatBO(
    val agentId: Long,
    val agentVo: AgentVO,
): Serializable
