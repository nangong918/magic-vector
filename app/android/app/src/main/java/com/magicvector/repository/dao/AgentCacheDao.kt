package com.magicvector.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.magicvector.domain.entity.AgentCacheEntity

@Dao
interface AgentCacheDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(entity: AgentCacheEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsertBatch(list: List<AgentCacheEntity>)

    @Query("SELECT * FROM agent_cache WHERE user_id = :userId ORDER BY updated_at DESC")
    suspend fun queryByUser(userId: Long): List<AgentCacheEntity>

    @Query("DELETE FROM agent_cache WHERE agent_id = :agentId")
    suspend fun deleteByAgentId(agentId: Long)
}