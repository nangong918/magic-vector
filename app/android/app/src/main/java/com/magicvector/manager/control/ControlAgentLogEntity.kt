package com.magicvector.manager.control

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "control_agent_log",
    indices = [
        Index(value = ["user_id", "agent_id", "log_time"], name = "idx_user_agent_log_time")
    ]
)
data class ControlAgentLogEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "agent_id")
    val agentId: Long,
    @ColumnInfo(name = "log_time")
    val logTime: Long,
    @ColumnInfo(name = "log_content")
    val logContent: String
)
