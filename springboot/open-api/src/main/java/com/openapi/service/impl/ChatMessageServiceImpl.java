package com.openapi.service.impl;

import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.mapper.ChatMessageMapper;
import com.openapi.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 13225
 * @date 2025/9/30 16:09
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    /**
     * 插入一条聊天记录
     * @param agentId   智能助手Id
     * @param message   消息
     * @param isUser    是否是用户
     * @return 消息Id
     */
    @Override
    public String insertOne(@NotNull String agentId, @NotNull String message, boolean isUser, String userId) {
        ChatMessageDo chatMessageDo = new ChatMessageDo();
        chatMessageDo.setAgentId(agentId);
        chatMessageDo.setContent(message);
        chatMessageDo.setRole(isUser ? 0 : 1);
        chatMessageDo.setUserId(userId);
        chatMessageDo.setChatTime(LocalDateTime.now());
        chatMessageMapper.insert(chatMessageDo);
        return chatMessageDo.getId();
    }

    /**
     * 查询指定时间之前的指定条数消息
     * @param agentId               智能助手Id
     * @param deadline              截止时间
     * @param limit                 查询条数
     * @return                      消息列表
     */
    @Override
    public List<ChatMessageDo> getMessagesByAgentIdDeadlineLimit(
            @NotNull String agentId,
            @NotNull LocalDateTime deadline,
            @NotNull Integer limit
    ){
        return chatMessageMapper.getMessagesByAgentIdDeadlineLimit(agentId, deadline, limit);
    }

    /**
     * 查询最新聊天记录20条; 会AOP先走Redis
     * @param agentId   智能助手Id
     * @return          聊天记录
     */
    @Cacheable
    @Override
    public List<ChatMessageDo> getLast20Messages(@NotNull String agentId){
        return chatMessageMapper.getMessagesByAgentIdDeadlineLimit(agentId, LocalDateTime.now(), 20);
    }

}
