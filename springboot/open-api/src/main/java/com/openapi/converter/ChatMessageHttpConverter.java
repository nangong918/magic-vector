package com.openapi.converter;

import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.dto.resonse.ChatMessageDto;
import com.openapi.utils.DateUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMessageHttpConverter {

    public List<ChatMessageDto> doListToDtoList(List<ChatMessageDo> chatMessageDos) {
        return chatMessageDos.stream().map(this::doToDto).toList();
    }

    public ChatMessageDto doToDto(ChatMessageDo item) {
        ChatMessageDto dto = new ChatMessageDto();
        if (item == null) {
            return dto;
        }
        Long id = item.getId() == null ? 0L : item.getId();
        dto.setId(String.valueOf(id));
        dto.setAgentId(String.valueOf(item.getAgentId() == null ? 0L : item.getAgentId()));
        dto.setUserId(String.valueOf(item.getUserId() == null ? 0L : item.getUserId()));
        Long messageId = item.getMessageId() == null ? id : item.getMessageId();
        dto.setMessageId(String.valueOf(messageId));
        dto.setTimestamp(String.valueOf(item.getChatTimestamp() == null ? 0L : item.getChatTimestamp()));
        dto.setImgUrl(item.getImgUrl() == null ? "" : item.getImgUrl());
        dto.setMessageType(item.getMessageType() == null ? 0 : item.getMessageType());
        dto.setContent(item.getContent() == null ? "" : item.getContent());
        dto.setRole(item.getRole() == null ? 0 : item.getRole());
        if (item.getChatTime() != null) {
            dto.setChatTime(DateUtils.yyyyMMddHHmmssToString(item.getChatTime()));
        }
        return dto;
    }
}
