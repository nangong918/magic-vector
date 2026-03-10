package com.magicvector.manager.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(list: List<ChatMessageEntity>)

    @Query("SELECT * FROM chat_message WHERE agent_id = :agentId ORDER BY chat_timestamp DESC, id DESC LIMIT :limit")
    suspend fun queryLastByAgent(agentId: Long, limit: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_message WHERE agent_id = :agentId AND chat_timestamp < :anchorTimestamp ORDER BY chat_timestamp DESC, id DESC LIMIT :limit")
    suspend fun queryByAnchorBefore(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_message WHERE agent_id = :agentId AND chat_timestamp > :anchorTimestamp ORDER BY chat_timestamp ASC, id ASC LIMIT :limit")
    suspend fun queryByAnchorAfter(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity>
}
