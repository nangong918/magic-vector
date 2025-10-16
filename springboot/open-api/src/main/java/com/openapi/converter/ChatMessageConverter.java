package com.openapi.converter;

import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.constant.RoleTypeEnum;
import com.openapi.domain.dto.ws.RealtimeChatTextResponse;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

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

    // List<ChatMessageDo> -> List<Message>
    @NonNull
    default List<Message> chatMessageDoListToMessageList(List<ChatMessageDo> chatMessageDos){
        List<Message> messages = new ArrayList<>();
        for (ChatMessageDo chatMessageDo : chatMessageDos) {
            // user message
            if (RoleTypeEnum.isUser(chatMessageDo.getRole())){
                UserMessage userMessage = new UserMessage(chatMessageDo.getContent());
                messages.add(userMessage);
            }
            // agent message
            else {
                AssistantMessage assistantMessage = new AssistantMessage(chatMessageDo.getContent());
                messages.add(assistantMessage);
            }
        }
        return messages;
    }
}
