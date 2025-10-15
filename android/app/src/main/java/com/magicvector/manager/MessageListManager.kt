package com.magicvector.manager

import com.core.baseutil.date.DateUtils
import com.data.domain.ao.agent.AgentChatAo
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.dto.response.AgentLastChatListResponse
import java.util.Optional

class MessageListManager {
    // request
    val agentChatAos: MutableList<AgentChatAo> = mutableListOf()
    // view
    val messageContactItemAos: MutableList<MessageContactItemAo> = mutableListOf()

    fun setAgentChatAos(response: AgentLastChatListResponse){
        agentChatAos.clear()
        response.agentChatAos?.let {
            agentChatAos.addAll(response.agentChatAos)
            messageContactItemAos.clear()
        }
        response.agentChatAos?.forEach {
            messageContactItemAos.add(responseToView(it))
        }
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
        agentChatAos.clear()
        messageContactItemAos.clear()
    }
}