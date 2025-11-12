package com.magicvector.manager

class ChatMapManager {

    private val chatManagers: MutableMap<String, ChatManager> = mutableMapOf()

    // 获取 ChatManager
    fun getChatManager(agentId: String): ChatManager {
        if (chatManagers[agentId] == null){
            chatManagers[agentId] = ChatManager(agentId)
        }
        return chatManagers[agentId]!!
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
        chatManagers.remove(agentId)
    }

    // 获取所有 ChatManagers
    fun getAllChatManagers(): List<ChatManager> {
        return chatManagers.values.toList()
    }
}