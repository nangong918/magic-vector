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
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgentChatEntity): Long

    /**
     * 批量插入/更新（主键冲突时替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(list: List<AgentChatEntity>): List<Long>

    /**
     * 根据用户ID查询聊天记录（按更新时间降序）
     */
    @Query("SELECT * FROM agent_chat WHERE user_id = :userId ORDER BY updated_at DESC")
    suspend fun queryByUser(userId: Long): List<AgentChatEntity>

    /**
     * 根据用户ID删除所有聊天记录
     */
    @Query("DELETE FROM agent_chat WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long)

    /**
     * 根据AgentID删除聊天记录
     */
    @Query("DELETE FROM agent_chat WHERE agent_id = :agentId")
    suspend fun deleteByAgentId(agentId: Long)

    // ========== 全量查询 ==========

    /**
     * 全量查询（按时间戳/UID字段）
     * @param userId 用户ID
     * @param orderBy 排序字段 (last_chat_time, updated_at, agent_id)
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param limit 查询条数
     */
    @Query("""
        SELECT * FROM agent_chat 
        WHERE user_id = :userId 
        ORDER BY 
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'last_chat_time' THEN last_chat_time END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'last_chat_time' THEN last_chat_time END DESC,
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'updated_at' THEN updated_at END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'updated_at' THEN updated_at END DESC,
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'agent_id' THEN agent_id END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'agent_id' THEN agent_id END DESC
        LIMIT :limit
    """)
    suspend fun queryFull(
        userId: Long,
        orderBy: String,
        sortOrder: String,
        limit: Int
    ): List<AgentChatEntity>

    /**
     * 全量查询（按名称排序）
     * @param userId 用户ID
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param limit 查询条数
     */
    @Query("""
    SELECT * FROM agent_chat 
    WHERE user_id = :userId 
    ORDER BY 
        CASE WHEN :sortOrder = 'ASC' THEN name END ASC,
        CASE WHEN :sortOrder = 'DESC' THEN name END DESC
    LIMIT :limit
    """)
    suspend fun queryFullByName(
        userId: Long,
        sortOrder: String,
        limit: Int
    ): List<AgentChatEntity>

    // ========== 分页查询 ==========

    /**
     * 分页查询（按时间戳/UID字段）
     * @param userId 用户ID
     * @param orderBy 排序字段 (last_chat_time, updated_at, agent_id)
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param pageDirection 分页方向 ("after" 或 "before")
     * @param cursor 游标值
     * @param limit 查询条数
     */
    @Query("""
        SELECT * FROM agent_chat 
        WHERE user_id = :userId 
        AND (
            (:pageDirection = 'after' AND :orderBy = 'last_chat_time' AND last_chat_time > :cursor) OR
            (:pageDirection = 'before' AND :orderBy = 'last_chat_time' AND last_chat_time < :cursor) OR
            (:pageDirection = 'after' AND :orderBy = 'updated_at' AND updated_at > :cursor) OR
            (:pageDirection = 'before' AND :orderBy = 'updated_at' AND updated_at < :cursor) OR
            (:pageDirection = 'after' AND :orderBy = 'agent_id' AND agent_id > :cursor) OR
            (:pageDirection = 'before' AND :orderBy = 'agent_id' AND agent_id < :cursor)
        )
        ORDER BY 
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'last_chat_time' THEN last_chat_time END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'last_chat_time' THEN last_chat_time END DESC,
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'updated_at' THEN updated_at END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'updated_at' THEN updated_at END DESC,
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'agent_id' THEN agent_id END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'agent_id' THEN agent_id END DESC
        LIMIT :limit
    """)
    suspend fun queryPage(
        userId: Long,
        orderBy: String,
        sortOrder: String,
        pageDirection: String,
        cursor: Long,
        limit: Int
    ): List<AgentChatEntity>

    /**
     * 分页查询（按名称）
     * @param userId 用户ID
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param pageDirection 分页方向 ("after" 或 "before")
     * @param cursor 游标值（名称字符串）
     * @param limit 查询条数
     */
    @Query("""
    SELECT * FROM agent_chat 
    WHERE user_id = :userId 
    AND (
        (:pageDirection = 'after' AND name > :cursor) OR
        (:pageDirection = 'before' AND name < :cursor)
    )
    ORDER BY 
        CASE WHEN :sortOrder = 'ASC' THEN name END ASC,
        CASE WHEN :sortOrder = 'DESC' THEN name END DESC
    LIMIT :limit
    """)
    suspend fun queryPageByName(
        userId: Long,
        sortOrder: String,
        pageDirection: String,
        cursor: String,
        limit: Int
    ): List<AgentChatEntity>
}