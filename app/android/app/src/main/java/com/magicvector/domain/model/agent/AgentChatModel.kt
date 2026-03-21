package com.magicvector.domain.model.agent

import com.magicvector.domain.vo.agent.AgentChatVo
import com.magicvector.utils.sort.SortItem
import com.magicvector.utils.sort.SortMode

/**
 * Agent Item 业务模型
 * @see com.magicvector.domain.entity.AgentChatEntity
 */
class AgentChatModel : SortItem {
    var agentChatVo: AgentChatVo? = null
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

    override fun getSortValue(mode: SortMode): Comparable<*> {
        // agent页面默认用name排序
        return getStringIndex()
    }
}