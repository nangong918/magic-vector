package com.magicvector.domain.convertor

import com.data.domain.constant.chat.RoleTypeEnum
import com.magicvector.domain.dto.http.response.AgentChatDto
import com.magicvector.domain.entity.AgentChatEntity
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.vo.agent.AgentChatVo
import com.magicvector.domain.vo.agent.AgentVo
import com.magicvector.domain.vo.message.ChatBriefMessageVo

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
            content = model.agentChatVo?.chatBriefMessageVo?.content ?: "",
            chatTime = model.agentChatVo?.chatBriefMessageVo?.chatTime ?: "",
            role = model.agentChatVo?.chatBriefMessageVo?.role ?: RoleTypeEnum.AGENT.value
        )
    }

    // entity -> model
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
            val chatBriefMessageVo = ChatBriefMessageVo(
                content = entity.content,
                chatTime = entity.chatTime,
                role = entity.role
            )

            // AgentChatVo
            agentChatVo = AgentChatVo(
                agentVo = agentVo,
                chatBriefMessageVo = chatBriefMessageVo,
                unreadCount = entity.unreadCount
            )
        }
    }

    // model -> dto
    fun model2Dto(model: AgentChatModel): AgentChatDto {
        return AgentChatDto(
            // AgentChatModel 基础字段
            agentId = model.agentId,
            userId = model.userId,
            lastChatTime = model.lastChatTime,
            updatedAt = model.updatedAt,

            // AgentChatVo 字段（空值兜底）
            unreadCount = model.agentChatVo?.unreadCount ?: 0,

            // AgentVo 字段（空值兜底）
            name = model.agentChatVo?.agentVo?.name ?: "",
            description = model.agentChatVo?.agentVo?.description ?: "",
            avatarUrl = model.agentChatVo?.agentVo?.avatarUrl,

            // ChatMessageVo 字段（空值兜底）
            content = model.agentChatVo?.chatBriefMessageVo?.content ?: "",
            chatTime = model.agentChatVo?.chatBriefMessageVo?.chatTime ?: "",
            role = model.agentChatVo?.chatBriefMessageVo?.role ?: RoleTypeEnum.AGENT.value
        )
    }

    // dto -> model
    fun dto2Model(dto: AgentChatDto): AgentChatModel {
        return AgentChatModel().apply {
            // 1. AgentChatModel 基础字段赋值
            agentId = dto.agentId
            userId = dto.userId
            lastChatTime = dto.lastChatTime
            updatedAt = dto.updatedAt

            // 2. 构建嵌套Vo对象
            val agentVo = AgentVo(
                name = dto.name,
                description = dto.description,
                avatarUrl = dto.avatarUrl
            )
            val chatBriefMessageVo = ChatBriefMessageVo(
                content = dto.content,
                chatTime = dto.chatTime,
                role = dto.role
            )

            // 3. 赋值AgentChatVo
            agentChatVo = AgentChatVo(
                agentVo = agentVo,
                chatBriefMessageVo = chatBriefMessageVo,
                unreadCount = dto.unreadCount
            )
        }
    }

    // dtos -> models
    fun dtos2Models(dtos: List<AgentChatDto>): List<AgentChatModel> {
        return dtos.map { dto ->
            dto2Model(dto)
        }
    }

}