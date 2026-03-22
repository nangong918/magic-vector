package com.magicvector.dataSource.local

import android.content.Context
import com.magicvector.dataSource.local.base.BaseLocalSource
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.convertor.ChatMessageConvertor
import com.magicvector.domain.entity.ChatMessageEntity
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.repository.dao.ChatMessageDao

class ChatLocalSource private constructor(
    context: Context
) : BaseLocalSource<ChatMessageModel, ChatMessageEntity, ChatMessageConvertor>() {

    private val chatDao: ChatMessageDao = VectorDatabase.getInstance(context).chatMessageDao()

    override val convertor: ChatMessageConvertor = ChatMessageConvertor

    // ========== 业务方法 ==========

    suspend fun upsertMessage(model: ChatMessageModel) = upsert(model) { chatDao.upsert(it) }
    suspend fun upsertMessages(models: List<ChatMessageModel>) = upsertBatch(models) { chatDao.upsertBatch(it) }
    suspend fun deleteByAgentId(agentId: Long) = delete { chatDao.deleteByAgentId(agentId) }
    suspend fun deleteByUserId(userId: Long) = delete { chatDao.deleteByUserId(userId) }
    suspend fun queryBySession(agentId: Long, userId: Long): List<ChatMessageModel> =
        queryList { chatDao.queryBySession(agentId, userId) }

    suspend fun queryFull(
        agentId: Long,
        userId: Long,
        orderBy: String,
        sortOrder: String,
        limit: Int
    ): List<ChatMessageModel> = queryList { chatDao.queryFull(agentId, userId, orderBy, sortOrder, limit) }

    suspend fun queryPage(
        agentId: Long,
        userId: Long,
        orderBy: String,
        sortOrder: String,
        pageDirection: String,
        cursor: Long,
        limit: Int
    ): List<ChatMessageModel> = queryList { chatDao.queryPage(agentId, userId, orderBy, sortOrder, pageDirection, cursor, limit) }

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