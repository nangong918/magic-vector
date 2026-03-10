package com.magicvector.manager

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class ChatMapController {
    companion object {
        // 防止会话数量异常膨胀占用内存；超出后淘汰最旧控制器。
        private const val MAX_CHAT_CONTROLLER_COUNT = 64
    }

    private val chatManagers: MutableMap<String, ChatController> = ConcurrentHashMap()
    private val createOrder: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    // 获取 ChatManager
    fun getChatManager(agentId: String): ChatController {
        val exist = chatManagers[agentId]
        if (exist != null) {
            return exist
        }
        val created = ChatController(agentId)
        val race = chatManagers.putIfAbsent(agentId, created)
        if (race == null) {
            createOrder.offer(agentId)
            trimControllersIfNeed()
            return created
        }
        return race
    }

    // 清除某个ChatManager的数据
    fun clearChatManagerData(agentId: String) {
        getChatManager(agentId).clear()
    }

    // 清除所有ChatManager的数据
    fun clearAllChatManagerData() {
        for (chatManager in chatManagers.values) {
            chatManager.clear()
        }
    }

    // 移除 ChatManager
    fun removeChatManager(agentId: String) {
        chatManagers.remove(agentId)?.clear()
        createOrder.remove(agentId)
    }

    // 获取所有 ChatManagers
    fun getAllChatManagers(): List<ChatController> {
        return chatManagers.values.toList()
    }

    private fun trimControllersIfNeed() {
        while (chatManagers.size > MAX_CHAT_CONTROLLER_COUNT) {
            val oldestKey = createOrder.poll() ?: break
            chatManagers.remove(oldestKey)?.clear()
        }
    }
}