package com.magicvector.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.magicvector.domain.entity.ChatMessageEntity

@Dao
interface ChatMessageDao {

    /**
     * 单条插入/更新（主键冲突时替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatMessageEntity): Long

    /**
     * 批量插入/更新（主键冲突时替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(list: List<ChatMessageEntity>): List<Long>

    /**
     * 根据会话ID查询聊天记录（按时间戳降序）
     * @param agentId AgentId
     * @param userId 用户Id
     */
    @Query("SELECT * FROM chat_message WHERE agent_id = :agentId AND user_id = :userId ORDER BY timestamp DESC")
    suspend fun queryBySession(agentId: Long, userId: Long): List<ChatMessageEntity>

    /**
     * 根据AgentID删除所有聊天记录
     */
    @Query("DELETE FROM chat_message WHERE agent_id = :agentId")
    suspend fun deleteByAgentId(agentId: Long)

    /**
     * 根据用户ID删除所有聊天记录
     */
    @Query("DELETE FROM chat_message WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long)

    // ========== 全量查询 ==========

    /**
     * 全量查询（按时间戳/消息ID字段）
     * @param agentId AgentId
     * @param userId 用户Id
     * @param orderBy 排序字段 (timestamp, message_id)
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param limit 查询条数
     */
    @Query("""
        SELECT * FROM chat_message 
        WHERE agent_id = :agentId AND user_id = :userId 
        ORDER BY 
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'timestamp' THEN timestamp END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'timestamp' THEN timestamp END DESC,
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'message_id' THEN message_id END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'message_id' THEN message_id END DESC
        LIMIT :limit
    """)
    suspend fun queryFull(
        agentId: Long,
        userId: Long,
        orderBy: String,
        sortOrder: String,
        limit: Int
    ): List<ChatMessageEntity>

    // ========== 分页查询 ==========

    /**
     * 分页查询（按时间戳/消息ID字段）
     * @param agentId AgentId
     * @param userId 用户Id
     * @param orderBy 排序字段 (timestamp, message_id)
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param pageDirection 分页方向 ("after" 或 "before")
     * @param cursor 游标值
     * @param limit 查询条数
     */
    @Query("""
        SELECT * FROM chat_message 
        WHERE agent_id = :agentId AND user_id = :userId 
        AND (
            (:pageDirection = 'after' AND :orderBy = 'timestamp' AND timestamp > :cursor) OR
            (:pageDirection = 'before' AND :orderBy = 'timestamp' AND timestamp < :cursor) OR
            (:pageDirection = 'after' AND :orderBy = 'message_id' AND message_id > :cursor) OR
            (:pageDirection = 'before' AND :orderBy = 'message_id' AND message_id < :cursor)
        )
        ORDER BY 
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'timestamp' THEN timestamp END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'timestamp' THEN timestamp END DESC,
            CASE WHEN :sortOrder = 'ASC' AND :orderBy = 'message_id' THEN message_id END ASC,
            CASE WHEN :sortOrder = 'DESC' AND :orderBy = 'message_id' THEN message_id END DESC
        LIMIT :limit
    """)
    suspend fun queryPage(
        agentId: Long,
        userId: Long,
        orderBy: String,
        sortOrder: String,
        pageDirection: String,
        cursor: Long,
        limit: Int
    ): List<ChatMessageEntity>
}