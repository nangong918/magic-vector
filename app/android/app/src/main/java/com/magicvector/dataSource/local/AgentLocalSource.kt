package com.magicvector.dataSource.local

import android.content.Context
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.convertor.AgentChatConvertor
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.repository.dao.AgentChatDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AgentChatModel数据源
 * 对上层提供Model屏蔽对Entity的感知
 */
class AgentLocalSource private constructor(
    context: Context
) {
    private val agentDao: AgentChatDao = VectorDatabase.getInstance(context).agentCacheDao()

    // ========== 写入操作 ==========

    /**
     * 全量替换用户的所有Agent（用于全量刷新）
     */
    suspend fun replaceAgentsByUser(
        userId: Long,
        models: List<AgentChatModel>
    ) = withContext(Dispatchers.IO) {
        agentDao.deleteByUserId(userId)
        if (models.isNotEmpty()) {
            val entities = models.map { AgentChatConvertor.model2Entity(it) }
            agentDao.upsertBatch(entities)
        }
    }

    /**
     * 单条插入/更新Agent
     */
    suspend fun upsertAgent(model: AgentChatModel) = withContext(Dispatchers.IO) {
        val entity = AgentChatConvertor.model2Entity(model)
        agentDao.upsert(entity)
    }

    /**
     * 批量插入/更新Agent
     */
    suspend fun upsertAgents(models: List<AgentChatModel>) = withContext(Dispatchers.IO) {
        if (models.isNotEmpty()) {
            val entities = models.map { AgentChatConvertor.model2Entity(it) }
            agentDao.upsertBatch(entities)
        }
    }

    /**
     * 删除单个Agent
     */
    suspend fun deleteAgent(agentId: Long) = withContext(Dispatchers.IO) {
        agentDao.deleteByAgentId(agentId)
    }

    /**
     * 删除用户的所有Agent
     */
    suspend fun deleteAgentsByUser(userId: Long) = withContext(Dispatchers.IO) {
        agentDao.deleteByUserId(userId)
    }

    // ========== 查询操作 ==========

    /**
     * 查询用户的所有Agent（按更新时间降序）
     */
    suspend fun queryAgentsByUser(userId: Long): List<AgentChatModel> = withContext(Dispatchers.IO) {
        val entities = agentDao.queryByUser(userId)
        entities.map { AgentChatConvertor.entity2Model(it) }
    }

    /**
     * 全量查询（按时间戳/UID字段）
     * @param userId 用户ID
     * @param orderBy 排序字段 (last_chat_time, updated_at, agent_id)
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param limit 查询条数
     */
    suspend fun queryFull(
        userId: Long,
        orderBy: String,
        sortOrder: String,
        limit: Int
    ): List<AgentChatModel> = withContext(Dispatchers.IO) {
        val entities = agentDao.queryFull(userId, orderBy, sortOrder, limit)
        entities.map { AgentChatConvertor.entity2Model(it) }
    }

    /**
     * 全量查询（按名称排序）
     * @param userId 用户ID
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param limit 查询条数
     */
    suspend fun queryFullByName(
        userId: Long,
        sortOrder: String,
        limit: Int
    ): List<AgentChatModel> = withContext(Dispatchers.IO) {
        val entities = agentDao.queryFullByName(userId, sortOrder, limit)
        entities.map { AgentChatConvertor.entity2Model(it) }
    }

    /**
     * 分页查询（按时间戳/UID字段）
     * @param userId 用户ID
     * @param orderBy 排序字段 (last_chat_time, updated_at, agent_id)
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param pageDirection 分页方向 ("after" 或 "before")
     * @param cursor 游标值
     * @param limit 查询条数
     */
    suspend fun queryPage(
        userId: Long,
        orderBy: String,
        sortOrder: String,
        pageDirection: String,
        cursor: Long,
        limit: Int
    ): List<AgentChatModel> = withContext(Dispatchers.IO) {
        val entities = agentDao.queryPage(userId, orderBy, sortOrder, pageDirection, cursor, limit)
        entities.map { AgentChatConvertor.entity2Model(it) }
    }

    /**
     * 分页查询（按名称）
     * @param userId 用户ID
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param pageDirection 分页方向 ("after" 或 "before")
     * @param cursor 游标值（名称字符串）
     * @param limit 查询条数
     */
    suspend fun queryPageByName(
        userId: Long,
        sortOrder: String,
        pageDirection: String,
        cursor: String,
        limit: Int
    ): List<AgentChatModel> = withContext(Dispatchers.IO) {
        val entities = agentDao.queryPageByName(userId, sortOrder, pageDirection, cursor, limit)
        entities.map { AgentChatConvertor.entity2Model(it) }
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