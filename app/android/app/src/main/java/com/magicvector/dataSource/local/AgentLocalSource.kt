package com.magicvector.dataSource.local

import android.content.Context
import com.magicvector.dataSource.local.base.BaseLocalSource
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.convertor.AgentChatConvertor
import com.magicvector.domain.entity.AgentChatEntity
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.repository.dao.AgentChatDao

class AgentLocalSource private constructor(
    context: Context
) : BaseLocalSource<AgentChatModel, AgentChatEntity, AgentChatConvertor>() {

    private val agentDao: AgentChatDao = VectorDatabase.getInstance(context).agentCacheDao()

    override val convertor: AgentChatConvertor = AgentChatConvertor

    // ========== 业务方法 ==========

    suspend fun replaceAgentsByUser(userId: Long, models: List<AgentChatModel>) = delete {
        agentDao.deleteByUserId(userId)
    }.let {
        if (models.isNotEmpty()) {
            upsertBatch(models) { agentDao.upsertBatch(it) }
        }
    }

    suspend fun upsertAgent(model: AgentChatModel) = upsert(model) { agentDao.upsert(it) }
    suspend fun upsertAgents(models: List<AgentChatModel>) = upsertBatch(models) { agentDao.upsertBatch(it) }
    suspend fun deleteAgent(agentId: Long) = delete { agentDao.deleteByAgentId(agentId) }
    suspend fun deleteAgentsByUser(userId: Long) = delete { agentDao.deleteByUserId(userId) }
    suspend fun queryAgentsByUser(userId: Long): List<AgentChatModel> = queryList { agentDao.queryByUser(userId) }

    suspend fun queryFull(userId: Long, orderBy: String, sortOrder: String, limit: Int): List<AgentChatModel> =
        queryList { agentDao.queryFull(userId, orderBy, sortOrder, limit) }

    suspend fun queryFullByName(userId: Long, sortOrder: String, limit: Int): List<AgentChatModel> =
        queryList { agentDao.queryFullByName(userId, sortOrder, limit) }

    suspend fun queryPage(
        userId: Long,
        orderBy: String,
        sortOrder: String,
        pageDirection: String,
        cursor: Long,
        limit: Int
    ): List<AgentChatModel> = queryList { agentDao.queryPage(userId, orderBy, sortOrder, pageDirection, cursor, limit) }

    suspend fun queryPageByName(
        userId: Long,
        sortOrder: String,
        pageDirection: String,
        cursor: String,
        limit: Int
    ): List<AgentChatModel> = queryList { agentDao.queryPageByName(userId, sortOrder, pageDirection, cursor, limit) }

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