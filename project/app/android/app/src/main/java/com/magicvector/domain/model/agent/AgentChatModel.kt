package com.magicvector.domain.model.agent

import com.magicvector.domain.vo.agent.AgentChatVO
import com.magicvector.utils.sort.SortItem

/**
 * Agent Item 业务模型
 * @see com.magicvector.domain.entity.AgentChatEntity
 */
class AgentChatModel : SortItem {
    var agentChatVo: AgentChatVO? = null
    var agentId: Long = 0L
    var userId: Long = 0L
    var lastChatTime: Long = 0L
    var updatedAt: Long = 0L

    override fun getUid(): Long {
        return agentId
    }

    override fun getTimestamp(): Long {
        return lastChatTime
    }

    override fun getStringIndex(): String {
        return agentChatVo?.agentVo?.name ?: throw NullPointerException("agentVo.name must not be null")
    }
}