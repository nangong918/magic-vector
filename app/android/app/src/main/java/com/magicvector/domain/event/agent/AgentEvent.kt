package com.magicvector.domain.event.agent

import com.magicvector.domain.event.EventSource

data class AgentEvent(
    val source: EventSource,
    val agentId: Long? = null
) : EventSource()