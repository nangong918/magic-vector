package com.magicvector.manager.chat

import android.content.Context
import com.data.domain.Do.ChatMessageEntity
import com.magicvector.domain.model.agent.AgentModel
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.model.message.MessageContactItemModel
import com.data.domain.vo.agent.AgentVo
import com.magicvector.dataSource.local.AgentLocalSource
import com.magicvector.dataSource.local.ChatLocalSource
import com.magicvector.domain.dto.ws.response.RealtimeChatTextResponse
import com.magicvector.domain.entity.AgentCacheEntity
import com.magicvector.domain.entity.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatCacheManager private constructor(context: Context) {
    private val agentLocalSource = AgentLocalSource.getInstance(context.applicationContext)
    private val chatLocalSource = ChatLocalSource.getInstance(context.applicationContext)

    suspend fun upsertAgent(entity: AgentCacheEntity) = withContext(Dispatchers.IO) {
        agentLocalSource.upsertAgent(entity)
    }

    suspend fun upsertAgents(list: List<AgentCacheEntity>) = withContext(Dispatchers.IO) {
        agentLocalSource.upsertAgents(list)
    }

    suspend fun upsertMessages(list: List<ChatMessageEntity>) = withContext(Dispatchers.IO) {
        chatLocalSource.upsertMessages(list)
    }

    suspend fun upsertRemoteMessages(list: List<ChatMessageEntity>) = withContext(Dispatchers.IO) {
        val entities = list.mapNotNull { it.toChatMessageEntity() }
        chatLocalSource.upsertMessages(entities)
    }

    suspend fun queryLastMessages(agentId: Long, limit: Int): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatLocalSource.queryLatestMessages(agentId, limit)
    }

    suspend fun queryBeforeAnchor(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            chatLocalSource.queryBeforeAnchor(agentId, anchorTimestamp, limit)
        }

    suspend fun queryAfterAnchor(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            chatLocalSource.queryAfterAnchor(agentId, anchorTimestamp, limit)
        }

    suspend fun syncHomeSnapshot(
        userId: Long,
        agents: List<AgentModel>,
        agentChats: List<AgentChatModel>
    ) = withContext(Dispatchers.IO) {
        val agentEntities = agents.mapNotNull { it.toAgentCacheEntity() }
        val chatEntities = agentChats.flatMap { agentChat ->
            agentChat.lastChatMessages.orEmpty().mapNotNull { chat ->
                chat.toChatMessageEntity()
            }
        }
        agentLocalSource.replaceAgentsByUser(userId, agentEntities)
        chatLocalSource.upsertMessages(chatEntities)
    }

    suspend fun queryHomeSnapshot(userId: Long): CachedHomeSnapshot = withContext(Dispatchers.IO) {
        val agentEntities = agentLocalSource.queryAgentsByUser(userId)
        val agents = agentEntities.map { it.toAgentAo() }
        val messageItems = agentEntities.mapNotNull { agentEntity ->
            val latestMessage = chatLocalSource.queryLatestMessages(agentEntity.agentId, 1).firstOrNull()
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
        chatLocalSource.appendRealtimeMessage(entity)
    }

    private fun AgentModel.toAgentCacheEntity(): AgentCacheEntity? {
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

    private fun AgentCacheEntity.toAgentAo(): AgentModel {
        return AgentModel().also { agent ->
            agent.agentId = agentId.toString()
            agent.userId = userId.toString()
            agent.agentVo = AgentVo().also { vo ->
                vo.name = name
                vo.description = description
                vo.avatarUrl = avatarUrl
            }
        }
    }

    private fun AgentCacheEntity.toMessageContactItemAo(message: ChatMessageEntity): MessageContactItemModel {
        return MessageContactItemModel().also { item ->
            item.contactId = agentId.toString()
            item.timestamp = message.chatTimestamp
            item.vo.name = name
            item.vo.avatarUrl = avatarUrl
            item.vo.setMessagePreview(message.content)
            item.vo.time = message.chatTime
            item.vo.unreadCount = 0
        }
    }

    private fun ChatMessageEntity.toChatMessageEntity(): ChatMessageEntity? {
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
    val agents: List<AgentModel>,
    val messageItems: List<MessageContactItemModel>
)
