package com.magicvector.manager.event.agent

import com.data.domain.ao.agent.AgentAo
import com.magicvector.manager.agent.AgentsManager
import com.magicvector.manager.event.AbstractEventManager
import com.magicvector.manager.event.EventSourceType

class AgentEventManager(
    private val compatibilityStore: AgentsManager? = null
) : AbstractEventManager<AgentAo, AgentEvent>() {

    fun replaceAll(
        list: List<AgentAo>,
        source: EventSourceType
    ): List<AgentAo> {
        val next = replaceAllInternal(
            list = list,
            event = AgentEvent.ReplaceAll(list = list, source = source)
        )
        syncCompatibilityStore(next)
        return next
    }

    fun upsert(
        agent: AgentAo,
        source: EventSourceType
    ): List<AgentAo> {
        val next = upsertTopInternal(
            item = agent,
            matcher = { it.agentId == agent.agentId },
            event = AgentEvent.UpsertOne(agent = agent, source = source)
        )
        syncCompatibilityStore(next)
        return next
    }

    fun remove(
        agentId: String,
        source: EventSourceType
    ): List<AgentAo> {
        val next = removeInternal(
            matcher = { it.agentId == agentId },
            event = AgentEvent.RemoveOne(agentId = agentId, source = source)
        )
        syncCompatibilityStore(next)
        return next
    }

    fun clear(source: EventSourceType): List<AgentAo> {
        val next = clearInternal(AgentEvent.Cleared(source = source))
        syncCompatibilityStore(next)
        return next
    }

    private fun syncCompatibilityStore(list: List<AgentAo>) {
        compatibilityStore?.setAgents(list)
    }
}

sealed class AgentEvent {
    data class ReplaceAll(
        val list: List<AgentAo>,
        val source: EventSourceType
    ) : AgentEvent()

    data class UpsertOne(
        val agent: AgentAo,
        val source: EventSourceType
    ) : AgentEvent()

    data class RemoveOne(
        val agentId: String,
        val source: EventSourceType
    ) : AgentEvent()

    data class Cleared(
        val source: EventSourceType
    ) : AgentEvent()
}
