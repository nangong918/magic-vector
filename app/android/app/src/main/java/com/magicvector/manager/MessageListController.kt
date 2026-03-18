package com.magicvector.manager

import com.core.baseutil.date.DateUtils
import com.data.domain.ao.agent.AgentChatAo
import com.data.domain.ao.message.MessageContactItemAo
import com.magicvector.MainApplication
import com.magicvector.domain.dto.http.response.AgentLastChatListResponse
import java.util.Optional

class MessageListController {
    // view
    val messageContactItemAos: MutableList<MessageContactItemAo> = mutableListOf()

    fun setAgentChatAos(response: AgentLastChatListResponse){
        setMessageContactItemAos(
            response.agentChatAos?.map { responseToView(it) }.orEmpty()
        )
    }

    fun setMessageContactItemAos(list: List<MessageContactItemAo>) {
        messageContactItemAos.clear()
        messageContactItemAos.addAll(list.sortedByDescending { it.timestamp })
    }

    fun upsertLatestMessage(
        agentId: String,
        preview: String,
        timestamp: Long,
        chatTime: String?
    ) {
        if (agentId.isBlank()) {
            return
        }
        val exist = messageContactItemAos.firstOrNull { it.contactId == agentId }
        if (exist != null) {
            exist.timestamp = timestamp
            exist.vo.setMessagePreview(preview)
            exist.vo.time = chatTime
        } else {
            val agent = MainApplication.getAgentsManager().agentList.value.firstOrNull { it.agentId == agentId }
                ?: return
            val created = MessageContactItemAo().apply {
                contactId = agentId
                this.timestamp = timestamp
                vo.name = agent.agentVo?.name.orEmpty()
                vo.avatarUrl = agent.agentVo?.avatarUrl
                vo.setMessagePreview(preview)
                vo.time = chatTime
                vo.unreadCount = 0
            }
            messageContactItemAos.add(created)
        }
        messageContactItemAos.sortByDescending { it.timestamp }
    }

    private fun responseToView(ao: AgentChatAo): MessageContactItemAo{
        val viewAo = MessageContactItemAo()

        // data
        viewAo.contactId = ao.agentAo?.agentId?: ""
        viewAo.timestamp = ao.lastChatTime

        // ItemVo
        viewAo.vo.avatarUrl = ao.agentAo?.agentVo?.avatarUrl?: ""
        viewAo.vo.name = ao.agentAo?.agentVo?.name?: ""
        val messagePreview : String = Optional.ofNullable(ao.lastChatMessages)
            .filter { it -> it.isNotEmpty() }
            .map { it -> it[0] }
            .map { it -> it.content }
            .orElse("")
        viewAo.vo.setMessagePreview(messagePreview)
        // timestamp -> yyyy-MM-dd HH:mm:ss
        val timeStr = DateUtils.getDateStringByTimestamp(
            ao.lastChatTime
        )
        viewAo.vo.time = timeStr
        viewAo.vo.unreadCount = ao.unreadCount

        return viewAo
    }

    fun clear() {
        messageContactItemAos.clear()
    }
}