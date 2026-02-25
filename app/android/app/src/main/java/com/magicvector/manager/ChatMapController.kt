package com.magicvector.manager

class ChatMapController {

    private val chatManagers: MutableMap<String, ChatController> = mutableMapOf()

    // 获取 ChatManager
    fun getChatManager(agentId: String): ChatController {
        if (chatManagers[agentId] == null){
            chatManagers[agentId] = ChatController(agentId)
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
    fun getAllChatManagers(): List<ChatController> {
        return chatManagers.values.toList()
    }
}