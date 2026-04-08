package com.magicvector.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.magicvector.domain.entity.ControlAgentLogEntity

@Dao
interface ControlAgentLogDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsertBatch(list: List<ControlAgentLogEntity>)

    @Query("SELECT * FROM control_agent_log WHERE user_id = :userId AND agent_id = :agentId ORDER BY log_time DESC, id DESC LIMIT :limit")
    suspend fun queryByAgentIdLimit(userId: Long, agentId: Long, limit: Int): List<ControlAgentLogEntity>

    @Query("SELECT * FROM control_agent_log WHERE user_id = :userId ORDER BY log_time DESC, id DESC LIMIT :limit")
    suspend fun queryByUserLimit(userId: Long, limit: Int): List<ControlAgentLogEntity>
}