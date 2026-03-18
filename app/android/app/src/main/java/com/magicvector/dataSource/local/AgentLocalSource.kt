package com.magicvector.dataSource.local

import android.content.Context
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.entity.AgentCacheEntity
import com.magicvector.repository.dao.AgentCacheDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentLocalSource private constructor(
    context: Context
) {
    private val agentDao: AgentCacheDao = VectorDatabase.getInstance(context).agentCacheDao()

    suspend fun replaceAgentsByUser(
        userId: Long,
        entities: List<AgentCacheEntity>
    ) = withContext(Dispatchers.IO) {
        agentDao.deleteByUserId(userId)
        if (entities.isNotEmpty()) {
            agentDao.upsertBatch(entities)
        }
    }

    suspend fun upsertAgent(entity: AgentCacheEntity) = withContext(Dispatchers.IO) {
        agentDao.upsert(entity)
    }

    suspend fun upsertAgents(entities: List<AgentCacheEntity>) = withContext(Dispatchers.IO) {
        if (entities.isNotEmpty()) {
            agentDao.upsertBatch(entities)
        }
    }

    suspend fun queryAgentsByUser(userId: Long): List<AgentCacheEntity> = withContext(Dispatchers.IO) {
        agentDao.queryByUser(userId)
    }

    suspend fun deleteAgent(agentId: Long) = withContext(Dispatchers.IO) {
        agentDao.deleteByAgentId(agentId)
    }

    companion object {
        @Volatile
        private var INSTANCE: AgentLocalSource? = null

        fun getInstance(context: Context): AgentLocalSource {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AgentLocalSource(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
