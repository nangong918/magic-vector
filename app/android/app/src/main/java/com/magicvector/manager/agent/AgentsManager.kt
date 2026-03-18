package com.magicvector.manager.agent

import com.data.domain.ao.agent.AgentAo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AgentsManager {
    private val _agentList = MutableStateFlow<List<AgentAo>>(emptyList())
    val agentList: StateFlow<List<AgentAo>> = _agentList.asStateFlow()

    // replay = 1: 表示当新的订阅者（collector）开始收集时，会立即收到最近发出的 1个 事件
    // extraBufferCapacity = 32: 最多可以缓存 32个 事件
    private val _effect = MutableSharedFlow<AgentsEffect>(replay = 1, extraBufferCapacity = 32)
    val effect: SharedFlow<AgentsEffect> = _effect.asSharedFlow()

    fun setAgents(list: List<AgentAo>) {
        _agentList.value = list
        _effect.tryEmit(AgentsEffect.AgentListChanged)
    }

    fun upsertAgent(agent: AgentAo?) {
        if (agent?.agentId.isNullOrBlank()) {
            return
        }
        val next = _agentList.value.toMutableList()
        val index = next.indexOfFirst { it.agentId == agent.agentId }
        if (index >= 0) {
            next[index] = agent
        } else {
            next.add(0, agent)
        }
        _agentList.value = next
        _effect.tryEmit(AgentsEffect.AgentListChanged)
    }

    fun removeAgent(agentId: String) {
        if (agentId.isBlank()) {
            return
        }
        _agentList.update { list -> list.filterNot { it.agentId == agentId } }
        _effect.tryEmit(AgentsEffect.AgentListChanged)
    }

    fun clear() {
        _agentList.value = emptyList()
        _effect.tryEmit(AgentsEffect.AgentListChanged)
    }
}

sealed class AgentsEffect {
    data object AgentListChanged : AgentsEffect()
}

