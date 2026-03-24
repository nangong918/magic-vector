package com.magicvector.domain.convertor

import com.magicvector.domain.constant.chat.RoleTypeEnum
import com.magicvector.domain.convertor.base.BaseConvertor
import com.magicvector.domain.dto.http.response.AgentChatDto
import com.magicvector.domain.entity.AgentChatEntity
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.vo.agent.AgentChatVO
import com.magicvector.domain.vo.agent.AgentVO
import com.magicvector.domain.vo.message.ChatBriefMessageVO

object AgentChatConvertor : BaseConvertor<AgentChatModel, AgentChatEntity, AgentChatDto>() {
    private fun String?.toLongOrZero(): Long = this?.toLongOrNull() ?: 0L

    override fun model2Entity(model: AgentChatModel, id: Long?): AgentChatEntity {
        return AgentChatEntity(
            id = id,
            agentId = model.agentId,
            userId = model.userId,
            lastChatTime = model.lastChatTime,
            updatedAt = model.updatedAt,
            unreadCount = model.agentChatVo?.unreadCount ?: 0,
            name = model.agentChatVo?.agentVo?.name ?: "",
            description = model.agentChatVo?.agentVo?.description ?: "",
            avatarUrl = model.agentChatVo?.agentVo?.avatarUrl,
            content = model.agentChatVo?.chatBriefMessageVo?.content ?: "",
            chatTime = model.agentChatVo?.chatBriefMessageVo?.chatTime ?: "",
            role = model.agentChatVo?.chatBriefMessageVo?.role ?: RoleTypeEnum.AGENT.value
        )
    }

    override fun entity2Model(entity: AgentChatEntity): AgentChatModel {
        return AgentChatModel().apply {
            agentId = entity.agentId
            userId = entity.userId
            lastChatTime = entity.lastChatTime
            updatedAt = entity.updatedAt

            val agentVo = AgentVO(
                name = entity.name,
                description = entity.description,
                avatarUrl = entity.avatarUrl
            )
            val chatBriefMessageVo = ChatBriefMessageVO(
                content = entity.content,
                chatTime = entity.chatTime,
                role = entity.role
            )

            agentChatVo = AgentChatVO(
                agentVo = agentVo,
                chatBriefMessageVo = chatBriefMessageVo,
                unreadCount = entity.unreadCount
            )
        }
    }

    override fun model2Dto(model: AgentChatModel): AgentChatDto {
        return AgentChatDto(
            agentId = model.agentId.toString(),
            userId = model.userId.toString(),
            lastChatTime = model.lastChatTime.toString(),
            updatedAt = model.updatedAt.toString(),
            unreadCount = model.agentChatVo?.unreadCount ?: 0,
            name = model.agentChatVo?.agentVo?.name ?: "",
            description = model.agentChatVo?.agentVo?.description ?: "",
            avatarUrl = model.agentChatVo?.agentVo?.avatarUrl,
            content = model.agentChatVo?.chatBriefMessageVo?.content ?: "",
            chatTime = model.agentChatVo?.chatBriefMessageVo?.chatTime ?: "",
            role = model.agentChatVo?.chatBriefMessageVo?.role ?: RoleTypeEnum.AGENT.value
        )
    }

    override fun dto2Model(dto: AgentChatDto): AgentChatModel {
        return AgentChatModel().apply {
            agentId = dto.agentId.toLongOrZero()
            userId = dto.userId.toLongOrZero()
            lastChatTime = dto.lastChatTime.toLongOrZero()
            updatedAt = dto.updatedAt.toLongOrZero()

            val agentVo = AgentVO(
                name = dto.name,
                description = dto.description,
                avatarUrl = dto.avatarUrl
            )
            val chatBriefMessageVo = ChatBriefMessageVO(
                content = dto.content,
                chatTime = dto.chatTime,
                role = dto.role
            )

            agentChatVo = AgentChatVO(
                agentVo = agentVo,
                chatBriefMessageVo = chatBriefMessageVo,
                unreadCount = dto.unreadCount
            )
        }
    }
}