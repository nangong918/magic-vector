package com.magicvector.domain.convertor

import com.magicvector.domain.constant.chat.MessageTypeEnum
import com.magicvector.domain.convertor.base.BaseConvertor
import com.magicvector.domain.dto.http.response.ChatMessageDto
import com.magicvector.domain.dto.ws.response.WsChatTextResponse
import com.magicvector.domain.entity.ChatMessageEntity
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVO
import com.magicvector.domain.vo.message.ChatMessageVO

object ChatMessageConvertor : BaseConvertor<ChatMessageModel, ChatMessageEntity, ChatMessageDto>() {
    private fun String?.toLongOrZero(): Long = this?.toLongOrNull() ?: 0L

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
        val briefMessageVo = ChatBriefMessageVO(
            content = entity.content,
            chatTime = entity.chatTime,
            role = entity.role
        )
        val chatMessageVo = ChatMessageVO(
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
            id = model.messageId.toString(),
            agentId = model.agentId.toString(),
            userId = model.userId.toString(),
            messageId = model.messageId.toString(),
            timestamp = model.timestamp.toString(),
            imgUrl = model.chatMessageVo.imgUrl,
            messageType = model.chatMessageVo.messageType,
            content = model.chatMessageVo.briefMessageVo.content,
            chatTime = model.chatMessageVo.briefMessageVo.chatTime,
            role = model.chatMessageVo.briefMessageVo.role
        )
    }

    override fun dto2Model(dto: ChatMessageDto): ChatMessageModel {
        val briefMessageVo = ChatBriefMessageVO(
            content = dto.content,
            chatTime = dto.chatTime,
            role = dto.role
        )
        val chatMessageVo = ChatMessageVO(
            briefMessageVo = briefMessageVo,
            imgUrl = dto.imgUrl,
            messageType = dto.messageType
        )
        return ChatMessageModel(
            chatMessageVo = chatMessageVo,
            agentId = dto.agentId.toLongOrZero(),
            userId = dto.userId.toLongOrZero(),
            messageId = dto.messageId.toLongOrZero(),
            timestamp = dto.timestamp.toLongOrZero()
        )
    }

    // WsChatTextResponse -> model
    fun wsChatTextResponse2Model(response: WsChatTextResponse): ChatMessageModel {
        val briefMessageVo = ChatBriefMessageVO(
            content = response.content,
            chatTime = response.chatTime,
            role = response.role
        )
        val chatMessageVo = ChatMessageVO(
            briefMessageVo = briefMessageVo,
            imgUrl = "",
            messageType = MessageTypeEnum.TEXT.value
        )
        return ChatMessageModel(
            chatMessageVo = chatMessageVo,
            agentId = response.agentId.toLong(),
            userId = response.userId.toLong(),
            messageId = response.messageId.toLong(),
            timestamp = response.timestamp
        )
    }
}