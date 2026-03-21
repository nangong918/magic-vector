package com.magicvector.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.magicvector.domain.entity.AgentChatEntity

/**
 * @see AgentChatEntity
 */
@Dao
interface AgentChatDao {
    /**
     * 单条插入/更新（主键冲突时替换）
     * @return 插入/更新后的自增主键ID（新增时返回生成的ID，更新时返回原ID）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgentChatEntity): Long

    /**
     * 批量插入/更新（主键冲突时替换）
     * @return 每条数据对应的自增主键ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(list: List<AgentChatEntity>): List<Long>

    /**
     * 根据用户ID查询聊天记录（按更新时间降序）
     * 修正：表名从 agent_cache → agent_chat（与Entity的tableName一致）
     */
    @Query("SELECT * FROM agent_chat WHERE user_id = :userId ORDER BY updated_at DESC")
    suspend fun queryByUser(userId: Long): List<AgentChatEntity>

    /**
     * 根据用户ID删除所有聊天记录
     * 修正：表名从 agent_cache → agent_chat
     */
    @Query("DELETE FROM agent_chat WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long)

    /**
     * 根据AgentID删除聊天记录
     * 修正：表名从 agent_cache → agent_chat
     */
    @Query("DELETE FROM agent_chat WHERE agent_id = :agentId")
    suspend fun deleteByAgentId(agentId: Long)
}