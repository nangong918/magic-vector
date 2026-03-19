package com.magicvector.manager

import com.core.baseutil.date.DateUtils
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.model.message.MessageContactItemModel
import com.magicvector.MainApplication
import com.magicvector.domain.dto.http.response.AgentLastChatListResponse
import java.util.Optional

class MessageListController {
    // view
    val messageContactItemModels: MutableList<MessageContactItemModel> = mutableListOf()

    fun setAgentChatAos(response: AgentLastChatListResponse){
        setMessageContactItemAos(
            response.agentChatModels?.map { responseToView(it) }.orEmpty()
        )
    }

    fun setMessageContactItemAos(list: List<MessageContactItemModel>) {
        messageContactItemModels.clear()
        messageContactItemModels.addAll(list.sortedByDescending { it.timestamp })
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
        val exist = messageContactItemModels.firstOrNull { it.contactId == agentId }
        if (exist != null) {
            exist.timestamp = timestamp
            exist.vo.setMessagePreview(preview)
            exist.vo.time = chatTime
        } else {
            val agent = MainApplication.getAgentsManager().agentList.value.firstOrNull { it.agentId == agentId }
                ?: return
            val created = MessageContactItemModel().apply {
                contactId = agentId
                this.timestamp = timestamp
                vo.name = agent.agentVo?.name.orEmpty()
                vo.avatarUrl = agent.agentVo?.avatarUrl
                vo.setMessagePreview(preview)
                vo.time = chatTime
                vo.unreadCount = 0
            }
            messageContactItemModels.add(created)
        }
        messageContactItemModels.sortByDescending { it.timestamp }
    }

    private fun responseToView(ao: AgentChatModel): MessageContactItemModel{
        val viewAo = MessageContactItemModel()

        // data
        viewAo.contactId = ao.agentModel?.agentId?: ""
        viewAo.timestamp = ao.lastChatTime

        // ItemVo
        viewAo.vo.avatarUrl = ao.agentModel?.agentVo?.avatarUrl?: ""
        viewAo.vo.name = ao.agentModel?.agentVo?.name?: ""
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
        messageContactItemModels.clear()
    }
}