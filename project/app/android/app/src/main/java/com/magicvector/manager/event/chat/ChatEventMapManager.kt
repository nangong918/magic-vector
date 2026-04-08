package com.magicvector.manager.event.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 聊天消息管理器映射
 * 管理多个 Agent 的 ChatEventManager 实例
 */
class ChatEventMapManager private constructor() {

    // 使用 ConcurrentHashMap 保证线程安全
    private val managers = ConcurrentHashMap<Long, ChatEventManager>()

    // 当前选中的 Agent ID
    private val _currentAgentId = MutableStateFlow<Long?>(null)
    val currentAgentId: StateFlow<Long?> = _currentAgentId.asStateFlow()

    // 当前选中的 Manager
    private val currentManager: ChatEventManager?
        get() = _currentAgentId.value?.let { managers[it] }

    /**
     * 获取或创建指定 Agent 的 ChatEventManager
     */
    fun getOrCreateManager(agentId: Long): ChatEventManager {
        return managers.getOrPut(agentId) {
            ChatEventManager(agentId)
        }
    }

    /**
     * 删除指定 Agent 的聊天管理器（可选，用于清理）
     */
    fun removeManager(agentId: Long) {
        // 先清理该 Manager 的缓存
        managers[agentId]?.clearCache()
        // 再从 Map 中移除
        managers.remove(agentId)
        // 如果当前选中的是这个 Agent，则清空当前选中
        if (_currentAgentId.value == agentId) {
            _currentAgentId.value = null
        }
    }

    /**
     * 清理所有管理器（可选，用于退出登录时）
     */
    fun clearAll() {
        // 先清理所有 Manager 的缓存
        managers.values.forEach { it.clearCache() }
        // 清空 Map
        managers.clear()
        // 清空当前选中的 Agent ID
        _currentAgentId.value = null
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatEventMapManager? = null

        fun getInstance(): ChatEventMapManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatEventMapManager().also { INSTANCE = it }
            }
        }
    }
}