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
    Integer delete(String id);

    Integer deleteBatch(@Param("list") List<String> ids);

    Integer deleteByAgentId(String agentId);

    Integer deleteByAgentIdBatch(@Param("list") List<String> agentIds);

    /// 查
    ChatMessageDo getById(String id);

    List<ChatMessageDo> getByIds(@Param("list") List<String> ids);

    /**
     * 获取指定时间之前的指定数量消息
     * @param agentId        代理id
     * @param deadline       截止时间
     * @param limit          数量
     * @return               消息
     */
    List<ChatMessageDo> getMessagesByAgentIdDeadlineLimit(
                @Param("agentId") String agentId,
                @Param("deadline")LocalDateTime deadline,
                @Param("limit") Integer limit
    );

    List<List<ChatMessageDo>> getMessageByAgentIds(
            @Param("agentIds") List<String> agentIds,
            @Param("deadline")LocalDateTime deadline,
            @Param("limit") Integer limit
    );

    List<ChatMessageDo> getAllMessagesByAgentId(String agentId);
}
