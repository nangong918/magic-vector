package com.magicvector.domain.convertor

import com.magicvector.domain.dto.http.response.ChatMessageDto
import com.magicvector.domain.entity.ChatMessageEntity
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.domain.vo.message.ChatBriefMessageVo
import com.magicvector.domain.vo.message.ChatMessageVo

object ChatMessageConvertor {

    // model -> Entity
    fun model2Entity(model: ChatMessageModel, id: Long? = null): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            // ChatMessageModel
            agentId = model.agentId,
            userId = model.userId,
            messageId = model.messageId,
            timestamp = model.timestamp,
            // ChatMessageVo
            imgUrl = model.chatMessageVo.imgUrl,
            messageType = model.chatMessageVo.messageType,
            // ChatBriefMessageVo
            content = model.chatMessageVo.briefMessageVo.content,
            chatTime = model.chatMessageVo.briefMessageVo.chatTime,
            role = model.chatMessageVo.briefMessageVo.role
        )
    }

    // entity -> model
    fun entity2Model(entity: ChatMessageEntity): ChatMessageModel {
        // ChatBriefMessageVo
        val briefMessageVo = ChatBriefMessageVo(
            content = entity.content,
            chatTime = entity.chatTime,
            role = entity.role
        )
        // ChatMessageVo
        val chatMessageVo = ChatMessageVo(
            briefMessageVo = briefMessageVo,
            imgUrl = entity.imgUrl,
            messageType = entity.messageType
        )
        // ChatMessageModel
        return ChatMessageModel(
            chatMessageVo = chatMessageVo,
            agentId = entity.agentId,
            userId = entity.userId,
            messageId = entity.messageId,
            timestamp = entity.timestamp
        )
    }

    // model -> dto
    fun model2Dto(model: ChatMessageModel): ChatMessageDto {
        return ChatMessageDto(
            // ChatMessageModel
            id = model.messageId,
            agentId = model.agentId,
            userId = model.userId,
            messageId = model.messageId,
            timestamp = model.timestamp,
            // ChatMessageVo
            imgUrl = model.chatMessageVo.imgUrl,
            messageType = model.chatMessageVo.messageType,
            // ChatBriefMessageVo
            content = model.chatMessageVo.briefMessageVo.content,
            chatTime = model.chatMessageVo.briefMessageVo.chatTime,
            role = model.chatMessageVo.briefMessageVo.role
        )
    }

    // dto -> model
    fun dto2Model(dto: ChatMessageDto): ChatMessageModel {
        // ChatBriefMessageVo
        val briefMessageVo = ChatBriefMessageVo(
            content = dto.content,
            chatTime = dto.chatTime,
            role = dto.role
        )
        // ChatMessageVo
        val chatMessageVo = ChatMessageVo(
            briefMessageVo = briefMessageVo,
            imgUrl = dto.imgUrl,
            messageType = dto.messageType
        )
        // ChatMessageModel
        return ChatMessageModel(
            chatMessageVo = chatMessageVo,
            agentId = dto.agentId,
            userId = dto.userId,
            messageId = dto.messageId,
            timestamp = dto.timestamp
        )
    }

    // dtos -> models
    fun dtos2Models(dtos: List<ChatMessageDto>): List<ChatMessageModel> {
        return dtos.map { dto2Model(it) }
    }

    // entities -> models
    fun entities2Models(entities: List<ChatMessageEntity>): List<ChatMessageModel> {
        return entities.map { entity2Model(it) }
    }

    // models -> dtos
    fun models2Dtos(models: List<ChatMessageModel>): List<ChatMessageDto> {
        return models.map { model2Dto(it) }
    }

    // models -> entities
    fun models2Entities(models: List<ChatMessageModel>): List<ChatMessageEntity> {
        return models.map { model2Entity(it) }
    }
}