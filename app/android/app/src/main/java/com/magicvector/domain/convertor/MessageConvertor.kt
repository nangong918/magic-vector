package com.magicvector.domain.convertor

import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.model.message.MessageContactItemModel
import com.data.domain.vo.message.MessageContactItemVo

object MessageConvertor {

    fun agentChatAo2MessageContactItemAo(agentChatModel: AgentChatModel) : MessageContactItemModel {
        val messageContactItemModel = MessageContactItemModel()
        messageContactItemModel.vo = MessageContactItemVo()
        agentChatModel.agentModel?.let {
            // vo
            it.agentVo?.let { vo ->
                messageContactItemModel.vo.avatarUrl = vo.avatarUrl
                messageContactItemModel.vo.name = vo.name
            }
            messageContactItemModel.vo.unreadCount = agentChatModel.unreadCount


            // ao
            messageContactItemModel.timestamp = agentChatModel.lastChatTime
            messageContactItemModel.contactId = it.agentId
        }
        agentChatModel.lastChatMessages?.let {
            if (it.isNotEmpty()) {
                agentChatModel.lastChatMessages[0].let { messageDo ->
                    messageContactItemModel.vo.setMessagePreview(messageDo.content)
                    messageContactItemModel.vo.time = messageDo.chatTime
                }
            }
        }

        return messageContactItemModel
    }

    fun agentChatAos2MessageContactItemAos(agentChatModels: List<AgentChatModel>) : List<MessageContactItemModel> {
        return agentChatModels.map { agentChatAo ->
            agentChatAo2MessageContactItemAo(agentChatAo)
        }
    }

}