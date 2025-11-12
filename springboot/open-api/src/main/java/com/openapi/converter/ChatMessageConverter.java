package com.openapi.converter;

import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.constant.RoleTypeEnum;
import com.openapi.domain.dto.ws.response.RealtimeChatTextResponse;
import com.openapi.utils.DateUtils;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
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
    default ChatMessageDo realtimeChatTextResponseToChatMessageDo(RealtimeChatTextResponse response, String chatTimeStr) throws Exception {
        if ( response == null ) {
            return null;
        }

        ChatMessageDo chatMessageDo = new ChatMessageDo();

        chatMessageDo.setId( response.getMessageId() );
        chatMessageDo.setAgentId( response.getAgentId() );
        chatMessageDo.setUserId( response.getUserId() );
        chatMessageDo.setContent( response.getContent() );
        chatMessageDo.setChatTimestamp( response.getTimestamp() );
        chatMessageDo.setRole( response.getRole() );

        LocalDateTime chatTime = DateUtils.getLocalDateTime(chatTimeStr, DateUtils.yyyyMMddHHmmss);
        chatMessageDo.setChatTime(chatTime);
        return chatMessageDo;
    }

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
