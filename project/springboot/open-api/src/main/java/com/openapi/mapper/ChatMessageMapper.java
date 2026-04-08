package com.openapi.mapper;

import com.openapi.domain.Do.ChatMessageDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 13225
 * @date 2025/9/30 15:52
 */
@Mapper
public interface ChatMessageMapper {

    /// 增
    Integer insert(ChatMessageDo chatMessageDo);

    Integer insertBatch(@Param("list") List<ChatMessageDo> chatMessageDos);

    /// 删
    Integer delete(Long id);

    Integer deleteBatch(@Param("list") List<Long> ids);

    Integer deleteByAgentId(Long agentId);

    Integer deleteByAgentIdBatch(@Param("list") List<Long> agentIds);

    /// 查
    ChatMessageDo getById(Long id);

    List<ChatMessageDo> getByIds(@Param("list") List<Long> ids);

    /**
     * 获取指定时间之前的指定数量消息
     * @param agentId        代理id
     * @param deadline       截止时间
     * @param limit          数量
     * @return               消息
     */
    List<ChatMessageDo> getMessagesByAgentIdDeadlineLimit(
                @Param("agentId") Long agentId,
                @Param("deadline")LocalDateTime deadline,
                @Param("limit") Integer limit
    );

    List<ChatMessageDo> getMessagesBeforeAnchorLimit(
            @Param("agentId") Long agentId,
            @Param("anchorTimestamp") Long anchorTimestamp,
            @Param("limit") Integer limit
    );

    List<ChatMessageDo> getMessagesAfterAnchorLimit(
            @Param("agentId") Long agentId,
            @Param("anchorTimestamp") Long anchorTimestamp,
            @Param("limit") Integer limit
    );

    List<ChatMessageDo> getMessageByAgentIds(
            @Param("agentIds") List<Long> agentIds,
            @Param("deadline")LocalDateTime deadline,
            @Param("limit") Integer limit
    );

    List<ChatMessageDo> getAllMessagesByAgentId(Long agentId);
}
