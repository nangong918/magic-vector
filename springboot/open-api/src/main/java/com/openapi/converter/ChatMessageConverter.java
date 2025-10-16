package com.openapi.converter;

import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.dto.ws.RealtimeChatTextResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

/**
 * @author 13225
 * @date 2025/10/16 11:47
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChatMessageConverter {

    ChatMessageConverter INSTANCE = Mappers.getMapper(ChatMessageConverter.class);

    // RealtimeChatTextResponse -> ChatMessageDo
    @Mapping(source = "messageId", target = "id")
    @Mapping(source = "agentId", target = "agentId")
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "chatTime", target = "chatTime")
    @Mapping(source = "timestamp", target = "chatTimestamp")
    @Mapping(source = "role", target = "role")
    ChatMessageDo realtimeChatTextResponseToChatMessageDo(RealtimeChatTextResponse realtimeChatTextResponse);

}
