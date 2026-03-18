package com.magicvector.manager.chat

import android.content.Context
import com.data.domain.Do.ChatMessageDo
import com.data.domain.ao.agent.AgentAo
import com.data.domain.ao.agent.AgentChatAo
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.vo.agent.AgentVo
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.dto.ws.response.RealtimeChatTextResponse
import com.magicvector.domain.entity.AgentCacheEntity
import com.magicvector.domain.entity.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatCacheManager private constructor(context: Context) {
    private val db = VectorDatabase.getInstance(context.applicationContext)
    private val chatDao = db.chatMessageDao()
    private val agentDao = db.agentCacheDao()

    suspend fun upsertAgent(entity: AgentCacheEntity) = withContext(Dispatchers.IO) {
        agentDao.upsert(entity)
    }

    suspend fun upsertAgents(list: List<AgentCacheEntity>) = withContext(Dispatchers.IO) {
        if (list.isNotEmpty()) {
            agentDao.upsertBatch(list)
        }
    }

    suspend fun upsertMessages(list: List<ChatMessageEntity>) = withContext(Dispatchers.IO) {
        if (list.isNotEmpty()) {
            chatDao.upsertBatch(list)
        }
    }

    suspend fun queryLastMessages(agentId: Long, limit: Int): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatDao.queryLastByAgent(agentId, limit)
    }

    suspend fun queryBeforeAnchor(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            chatDao.queryByAnchorBefore(agentId, anchorTimestamp, limit)
        }

    suspend fun queryAfterAnchor(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            chatDao.queryByAnchorAfter(agentId, anchorTimestamp, limit)
        }

    suspend fun syncHomeSnapshot(
        userId: Long,
        agents: List<AgentAo>,
        agentChats: List<AgentChatAo>
    ) = withContext(Dispatchers.IO) {
        val agentEntities = agents.mapNotNull { it.toAgentCacheEntity() }
        val chatEntities = agentChats.flatMap { agentChat ->
            agentChat.lastChatMessages.orEmpty().mapNotNull { chat ->
                chat.toChatMessageEntity()
            }
        }
        agentDao.deleteByUserId(userId)
        if (agentEntities.isNotEmpty()) {
            agentDao.upsertBatch(agentEntities)
        }
        if (chatEntities.isNotEmpty()) {
            chatDao.upsertBatch(chatEntities)
        }
    }

    suspend fun queryHomeSnapshot(userId: Long): CachedHomeSnapshot = withContext(Dispatchers.IO) {
        val agentEntities = agentDao.queryByUser(userId)
        val agents = agentEntities.map { it.toAgentAo() }
        val messageItems = agentEntities.mapNotNull { agentEntity ->
            val latestMessage = chatDao.queryLastByAgent(agentEntity.agentId, 1).firstOrNull()
                ?: return@mapNotNull null
            agentEntity.toMessageContactItemAo(latestMessage)
        }.sortedByDescending { it.timestamp }
        CachedHomeSnapshot(
            agents = agents,
            messageItems = messageItems
        )
    }

    suspend fun upsertRealtimeMessage(
        response: RealtimeChatTextResponse,
        resolvedContent: String? = null,
        resolvedChatTime: String? = null,
        resolvedTimestamp: Long? = null
    ) = withContext(Dispatchers.IO) {
        val entity = response.toChatMessageEntity(
            content = resolvedContent,
            chatTime = resolvedChatTime,
            timestamp = resolvedTimestamp
        ) ?: return@withContext
        chatDao.upsert(entity)
    }

    private fun AgentAo.toAgentCacheEntity(): AgentCacheEntity? {
        val parsedAgentId = agentId?.toLongOrNull() ?: return null
        val parsedUserId = userId?.toLongOrNull() ?: return null
        return AgentCacheEntity(
            id = parsedAgentId,
            agentId = parsedAgentId,
            userId = parsedUserId,
            name = agentVo?.name.orEmpty(),
            description = agentVo?.description.orEmpty(),
            avatarUrl = agentVo?.avatarUrl,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun AgentCacheEntity.toAgentAo(): AgentAo {
        return AgentAo().also { agent ->
            agent.agentId = agentId.toString()
            agent.userId = userId.toString()
            agent.agentVo = AgentVo().also { vo ->
                vo.name = name
                vo.description = description
                vo.avatarUrl = avatarUrl
            }
        }
    }

    private fun AgentCacheEntity.toMessageContactItemAo(message: ChatMessageEntity): MessageContactItemAo {
        return MessageContactItemAo().also { item ->
            item.contactId = agentId.toString()
            item.timestamp = message.chatTimestamp
            item.vo.name = name
            item.vo.avatarUrl = avatarUrl
            item.vo.setMessagePreview(message.content)
            item.vo.time = message.chatTime
            item.vo.unreadCount = 0
        }
    }

    private fun ChatMessageDo.toChatMessageEntity(): ChatMessageEntity? {
        val parsedId = id?.toLongOrNull() ?: return null
        val parsedAgentId = agentId?.toLongOrNull() ?: return null
        val parsedUserId = userId?.toLongOrNull() ?: return null
        return ChatMessageEntity(
            id = parsedId,
            agentId = parsedAgentId,
            userId = parsedUserId,
            content = content.orEmpty(),
            chatTimestamp = chatTimestamp ?: 0L,
            chatTime = chatTime.orEmpty(),
            role = role ?: 0,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun RealtimeChatTextResponse.toChatMessageEntity(
        content: String? = null,
        chatTime: String? = null,
        timestamp: Long? = null
    ): ChatMessageEntity? {
        val parsedId = messageId?.toLongOrNull() ?: return null
        val parsedAgentId = agentId?.toLongOrNull() ?: return null
        val parsedUserId = userId?.toLongOrNull() ?: return null
        return ChatMessageEntity(
            id = parsedId,
            agentId = parsedAgentId,
            userId = parsedUserId,
            content = content ?: this.content.orEmpty(),
            chatTimestamp = timestamp ?: this.timestamp ?: 0L,
            chatTime = chatTime ?: this.chatTime.orEmpty(),
            role = role ?: 0,
            createdAt = System.currentTimeMillis()
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatCacheManager? = null

        fun getInstance(context: Context): ChatCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatCacheManager(context).also { INSTANCE = it }
            }
        }
    }
}

data class CachedHomeSnapshot(
    val agents: List<AgentAo>,
    val messageItems: List<MessageContactItemAo>
)
