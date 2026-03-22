package com.magicvector.domain.convertor

import com.magicvector.domain.convertor.base.BaseConvertor
import com.magicvector.domain.dto.http.response.ChatMessageDto
import com.magicvector.domain.entity.ChatMessageEntity
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVo
import com.magicvector.domain.vo.message.ChatMessageVo

object ChatMessageConvertor : BaseConvertor<ChatMessageModel, ChatMessageEntity, ChatMessageDto>() {

    override fun model2Entity(model: ChatMessageModel, id: Long?): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            agentId = model.agentId,
            userId = model.userId,
            messageId = model.messageId,
            timestamp = model.timestamp,
            imgUrl = model.chatMessageVo.imgUrl,
            messageType = model.chatMessageVo.messageType,
            content = model.chatMessageVo.briefMessageVo.content,
            chatTime = model.chatMessageVo.briefMessageVo.chatTime,
            role = model.chatMessageVo.briefMessageVo.role
        )
    }

    override fun entity2Model(entity: ChatMessageEntity): ChatMessageModel {
        val briefMessageVo = ChatBriefMessageVo(
            content = entity.content,
            chatTime = entity.chatTime,
            role = entity.role
        )
        val chatMessageVo = ChatMessageVo(
            briefMessageVo = briefMessageVo,
            imgUrl = entity.imgUrl,
            messageType = entity.messageType
        )
        return ChatMessageModel(
            chatMessageVo = chatMessageVo,
            agentId = entity.agentId,
            userId = entity.userId,
            messageId = entity.messageId,
            timestamp = entity.timestamp
        )
    }

    override fun model2Dto(model: ChatMessageModel): ChatMessageDto {
        return ChatMessageDto(
            id = model.messageId,
            agentId = model.agentId,
            userId = model.userId,
            messageId = model.messageId,
            timestamp = model.timestamp,
            imgUrl = model.chatMessageVo.imgUrl,
            messageType = model.chatMessageVo.messageType,
            content = model.chatMessageVo.briefMessageVo.content,
            chatTime = model.chatMessageVo.briefMessageVo.chatTime,
            role = model.chatMessageVo.briefMessageVo.role
        )
    }

    override fun dto2Model(dto: ChatMessageDto): ChatMessageModel {
        val briefMessageVo = ChatBriefMessageVo(
            content = dto.content,
            chatTime = dto.chatTime,
            role = dto.role
        )
        val chatMessageVo = ChatMessageVo(
            briefMessageVo = briefMessageVo,
            imgUrl = dto.imgUrl,
            messageType = dto.messageType
        )
        return ChatMessageModel(
            chatMessageVo = chatMessageVo,
            agentId = dto.agentId,
            userId = dto.userId,
            messageId = dto.messageId,
            timestamp = dto.timestamp
        )
    }
}