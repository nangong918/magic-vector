package com.magicvector.manager.event.chat

import com.magicvector.domain.model.chat.ChatItemModel
import com.magicvector.domain.model.message.MessageContactItemModel
import com.magicvector.manager.event.AbstractEventManager
import com.magicvector.manager.event.EventSourceType

class ChatEventManager : AbstractEventManager<ChatSessionState, ChatEvent>() {

    fun replaceConversation(
        agentId: String,
        messages: List<ChatItemModel>,
        summary: MessageContactItemModel?,
        source: EventSourceType
    ): List<ChatSessionState> {
        val session = ChatSessionState(
            agentId = agentId,
            messages = messages.sortedByDescending { it.timestamp },
            summary = summary
        )
        return upsertTopInternal(
            item = session,
            matcher = { it.agentId == agentId },
            event = ChatEvent.ReplaceConversation(
                agentId = agentId,
                messages = session.messages,
                summary = summary,
                source = source
            )
        )
    }

    fun insertHistoryPage(
        agentId: String,
        history: List<ChatItemModel>,
        source: EventSourceType
    ): List<ChatSessionState> {
        val current = getSession(agentId)
        if (current == null) {
            return replaceConversation(
                agentId = agentId,
                messages = history,
                summary = null,
                source = source
            )
        }
        val merged = (current.messages + history)
            .distinctBy { it.messageId ?: "${it.senderId}_${it.timestamp}" }
            .sortedByDescending { it.timestamp }
        return replaceConversation(
            agentId = agentId,
            messages = merged,
            summary = current.summary,
            source = source
        )
    }

    fun appendRealtimeMessage(
        agentId: String,
        item: ChatItemModel,
        summary: MessageContactItemModel?,
        source: EventSourceType
    ): List<ChatSessionState> {
        val current = getSession(agentId)
        val merged = ((current?.messages ?: emptyList()) + item)
            .distinctBy { it.messageId ?: "${it.senderId}_${it.timestamp}" }
            .sortedByDescending { it.timestamp }
        return upsertTopInternal(
            item = ChatSessionState(
                agentId = agentId,
                messages = merged,
                summary = summary ?: current?.summary
            ),
            matcher = { it.agentId == agentId },
            event = ChatEvent.AppendRealtimeMessage(
                agentId = agentId,
                message = item,
                summary = summary ?: current?.summary,
                source = source
            )
        )
    }

    fun replaceSummaries(
        list: List<MessageContactItemModel>,
        source: EventSourceType
    ): List<ChatSessionState> {
        val currentMap = snapshotItems().associateBy { it.agentId }
        val updatedSessions = list.map { summary ->
            val agentId = summary.contactId.orEmpty()
            val current = currentMap[agentId]
            ChatSessionState(
                agentId = agentId,
                messages = current?.messages.orEmpty(),
                summary = summary
            )
        }
        val untouchedSessions = snapshotItems().filter { session ->
            updatedSessions.none { it.agentId == session.agentId }
        }
        return replaceAllInternal(
            list = (updatedSessions + untouchedSessions).sortedByDescending { it.summary?.timestamp ?: 0L },
            event = ChatEvent.ReplaceSummaries(
                summaries = list,
                source = source
            )
        )
    }

    fun getSession(agentId: String): ChatSessionState? {
        return snapshotItems().firstOrNull { it.agentId == agentId }
    }

    fun getSummaries(): List<MessageContactItemModel> {
        return snapshotItems()
            .mapNotNull { it.summary }
            .sortedByDescending { it.timestamp }
    }
}

data class ChatSessionState(
    val agentId: String,
    val messages: List<ChatItemModel>,
    val summary: MessageContactItemModel?
)

sealed class ChatEvent {
    data class ReplaceConversation(
        val agentId: String,
        val messages: List<ChatItemModel>,
        val summary: MessageContactItemModel?,
        val source: EventSourceType
    ) : ChatEvent()

    data class AppendRealtimeMessage(
        val agentId: String,
        val message: ChatItemModel,
        val summary: MessageContactItemModel?,
        val source: EventSourceType
    ) : ChatEvent()

    data class InsertHistoryPage(
        val agentId: String,
        val messages: List<ChatItemModel>,
        val source: EventSourceType
    ) : ChatEvent()

    data class ReplaceSummaries(
        val summaries: List<MessageContactItemModel>,
        val source: EventSourceType
    ) : ChatEvent()
}
