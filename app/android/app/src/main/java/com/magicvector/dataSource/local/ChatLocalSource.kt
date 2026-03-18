package com.magicvector.dataSource.local

import android.content.Context
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.entity.ChatMessageEntity
import com.magicvector.repository.dao.ChatMessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatLocalSource private constructor(
    context: Context
) {
    private val chatDao: ChatMessageDao = VectorDatabase.getInstance(context).chatMessageDao()

    suspend fun replaceLatestMessages(
        messages: List<ChatMessageEntity>
    ) = withContext(Dispatchers.IO) {
        if (messages.isNotEmpty()) {
            chatDao.upsertBatch(messages)
        }
    }

    suspend fun appendRealtimeMessage(message: ChatMessageEntity) = withContext(Dispatchers.IO) {
        chatDao.upsert(message)
    }

    suspend fun upsertMessages(messages: List<ChatMessageEntity>) = withContext(Dispatchers.IO) {
        if (messages.isNotEmpty()) {
            chatDao.upsertBatch(messages)
        }
    }

    suspend fun queryLatestMessages(agentId: Long, limit: Int): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatDao.queryLastByAgent(agentId, limit)
    }

    suspend fun queryBeforeAnchor(
        agentId: Long,
        anchorTimestamp: Long,
        limit: Int
    ): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatDao.queryByAnchorBefore(agentId, anchorTimestamp, limit)
    }

    suspend fun queryAfterAnchor(
        agentId: Long,
        anchorTimestamp: Long,
        limit: Int
    ): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatDao.queryByAnchorAfter(agentId, anchorTimestamp, limit)
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
