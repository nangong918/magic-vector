package com.magicvector.domain.convertor

import com.data.domain.ao.agent.AgentChatAo
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.vo.message.MessageContactItemVo

object MessageConvertor {

    fun agentChatAo2MessageContactItemAo(agentChatAo: AgentChatAo) : MessageContactItemAo {
        val messageContactItemAo = MessageContactItemAo()
        messageContactItemAo.vo = MessageContactItemVo()
        agentChatAo.agentAo?.let {
            // vo
            it.agentVo?.let { vo ->
                messageContactItemAo.vo.avatarUrl = vo.avatarUrl
                messageContactItemAo.vo.name = vo.name
            }
            messageContactItemAo.vo.unreadCount = agentChatAo.unreadCount


            // ao
            messageContactItemAo.timestamp = agentChatAo.lastChatTime
            messageContactItemAo.contactId = it.agentId
        }
        agentChatAo.lastChatMessages?.let {
            if (it.isNotEmpty()) {
                agentChatAo.lastChatMessages[0].let { messageDo ->
                    messageContactItemAo.vo.setMessagePreview(messageDo.content)
                    messageContactItemAo.vo.time = messageDo.chatTime
                }
            }
        }

        return messageContactItemAo
    }

    fun agentChatAos2MessageContactItemAos(agentChatAos: List<AgentChatAo>) : List<MessageContactItemAo> {
        return agentChatAos.map { agentChatAo ->
            agentChatAo2MessageContactItemAo(agentChatAo)
        }
    }

}