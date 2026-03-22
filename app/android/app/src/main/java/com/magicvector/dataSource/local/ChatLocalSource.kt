package com.magicvector.dataSource.local

import android.content.Context
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.convertor.ChatMessageConvertor
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.repository.dao.ChatMessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ChatMessageModel数据源
 * 对上层提供Model屏蔽对Entity的感知
 * todo：抽离通用Abstract类
 */
class ChatLocalSource private constructor(
    context: Context
) {
    private val chatDao: ChatMessageDao = VectorDatabase.getInstance(context).chatMessageDao()

    // ========== 写入操作 ==========

    /**
     * 单条插入/更新消息
     */
    suspend fun upsertMessage(model: ChatMessageModel) = withContext(Dispatchers.IO) {
        val entity = ChatMessageConvertor.model2Entity(model)
        chatDao.upsert(entity)
    }

    /**
     * 批量插入/更新消息
     */
    suspend fun upsertMessages(models: List<ChatMessageModel>) = withContext(Dispatchers.IO) {
        if (models.isNotEmpty()) {
            val entities = models.map { ChatMessageConvertor.model2Entity(it) }
            chatDao.upsertBatch(entities)
        }
    }

    /**
     * 删除会话的所有消息
     */
    suspend fun deleteByAgentId(agentId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteByAgentId(agentId)
    }

    /**
     * 删除用户的所有消息
     */
    suspend fun deleteByUserId(userId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteByUserId(userId)
    }

    // ========== 查询操作 ==========

    /**
     * 查询会话的所有消息（按时间戳降序）
     */
    suspend fun queryBySession(agentId: Long, userId: Long): List<ChatMessageModel> = withContext(Dispatchers.IO) {
        val entities = chatDao.queryBySession(agentId, userId)
        entities.map { ChatMessageConvertor.entity2Model(it) }
    }

    /**
     * 全量查询（按时间戳/消息ID字段）
     * @param agentId AgentId
     * @param userId 用户Id
     * @param orderBy 排序字段 (timestamp, message_id)
     * @param sortOrder 排序顺序 ("ASC" 或 "DESC")
     * @param limit 查询条数
     */
    suspend fun queryFull(
        agentId: Long,
        userId: Long,
        orderBy: String,
        sortOrder: String,
        limit: Int
    ): List<ChatMessageModel> = withContext(Dispatchers.IO) {
        val entities = chatDao.queryFull(agentId, userId, orderBy, sortOrder, limit)
        entities.map { ChatMessageConvertor.entity2Model(it) }
    }

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
    suspend fun queryPage(
        agentId: Long,
        userId: Long,
        orderBy: String,
        sortOrder: String,
        pageDirection: String,
        cursor: Long,
        limit: Int
    ): List<ChatMessageModel> = withContext(Dispatchers.IO) {
        val entities = chatDao.queryPage(agentId, userId, orderBy, sortOrder, pageDirection, cursor, limit)
        entities.map { ChatMessageConvertor.entity2Model(it) }
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatLocalSource? = null

        fun getInstance(context: Context): ChatLocalSource {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatLocalSource(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}