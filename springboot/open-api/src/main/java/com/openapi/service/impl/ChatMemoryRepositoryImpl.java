package com.openapi.service.impl;

import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.mapper.AgentMapper;
import com.openapi.mapper.ChatMessageMapper;
import com.openapi.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
//import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 13225
 * @date 2025/10/14 17:46
 */
@Slf4j
@RequiredArgsConstructor
//@Service
public class ChatMemoryRepositoryImpl implements ChatMemoryRepository {

    private final ChatMessageService chatMessageService;
    private final ChatMessageMapper chatMessageMapper;
    private final AgentMapper agentMapper;

    @NotNull
    @Override
    public List<String> findConversationIds() {
        return agentMapper.selectAllIds();
    }

    @NotNull
    @Override
    public List<Message> findByConversationId(@NotNull String agentId) {
        List<ChatMessageDo> chatMessageDos = chatMessageMapper.getAllMessagesByAgentId(agentId);
        /*
            暂停开发，这种方式本身就不对，应该使用官方组件。
            Message是抽象类，分为很多种类：
            AbstractMessage
            AssistantMessage
            SystemMessage
            ToolResponseMessage
            UserMessage
        */
        return List.of();
    }

    @Override
    public void saveAll(@NotNull String agentId, @NotNull List<Message> messages) {

    }

    @Override
    public void deleteByConversationId(@NotNull String agentId) {

    }
}
