package com.magicvector.domain.convertor

import com.data.domain.constant.chat.RoleTypeEnum
import com.magicvector.domain.entity.AgentChatEntity
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.vo.agent.AgentChatVo
import com.magicvector.domain.vo.agent.AgentVo
import com.magicvector.domain.vo.message.ChatMessageVo

object AgentChatConvertor {


    // model -> Entity
    fun model2Entity(model: AgentChatModel, id: Long? = null): AgentChatEntity {
        return AgentChatEntity(
            id = id,
            // AgentChatModel
            agentId = model.agentId,
            userId = model.userId,
            lastChatTime = model.lastChatTime,
            updatedAt = model.updatedAt,

            // AgentChatVo
            unreadCount = model.agentChatVo?.unreadCount ?: 0,

            // AgentVo
            name = model.agentChatVo?.agentVo?.name ?: "",
            description = model.agentChatVo?.agentVo?.description ?: "",
            avatarUrl = model.agentChatVo?.agentVo?.avatarUrl,

            // ChatMessageVo
            content = model.agentChatVo?.chatMessageVo?.content ?: "",
            chatTime = model.agentChatVo?.chatMessageVo?.chatTime ?: "",
            role = model.agentChatVo?.chatMessageVo?.role ?: RoleTypeEnum.AGENT.value
        )
    }

    fun entity2Model(entity: AgentChatEntity): AgentChatModel {
        return AgentChatModel().apply {
            // 1. AgentChatModel 基础字段赋值（Kotlin 可空/可变属性适配）
            agentId = entity.agentId
            userId = entity.userId
            lastChatTime = entity.lastChatTime
            updatedAt = entity.updatedAt

            // AgentVo
            val agentVo = AgentVo(
                name = entity.name,
                description = entity.description,
                avatarUrl = entity.avatarUrl
            )
            // ChatMessageVo
            val chatMessageVo = ChatMessageVo(
                content = entity.content,
                chatTime = entity.chatTime,
                role = entity.role
            )

            // AgentChatVo
            agentChatVo = AgentChatVo(
                agentVo = agentVo,
                chatMessageVo = chatMessageVo,
                unreadCount = entity.unreadCount
            )
        }
    }

}