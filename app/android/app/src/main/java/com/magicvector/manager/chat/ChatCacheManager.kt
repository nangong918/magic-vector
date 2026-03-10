package com.magicvector.manager.chat

import android.content.Context
import com.magicvector.manager.db.VectorDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatCacheManager private constructor(context: Context) {
    private val db = VectorDatabase.getInstance(context.applicationContext)
    private val chatDao = db.chatMessageDao()
    private val agentDao = db.agentCacheDao()

    suspend fun upsertAgent(entity: AgentCacheEntity) = withContext(Dispatchers.IO) {
        agentDao.upsert(entity)
    }

    suspend fun upsertMessages(list: List<ChatMessageEntity>) = withContext(Dispatchers.IO) {
        if (list.isNotEmpty()) {
            chatDao.upsertBatch(list)
        }
    }

    suspend fun queryLastMessages(agentId: Long, limit: Int): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatDao.queryLastByAgent(agentId, limit)
    }

    suspend fun queryBeforeAnchor(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            chatDao.queryByAnchorBefore(agentId, anchorTimestamp, limit)
        }

    suspend fun queryAfterAnchor(agentId: Long, anchorTimestamp: Long, limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            chatDao.queryByAnchorAfter(agentId, anchorTimestamp, limit)
        }

    companion object {
        @Volatile
        private var INSTANCE: ChatCacheManager? = null

        fun getInstance(context: Context): ChatCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatCacheManager(context).also { INSTANCE = it }
            }
        }
    }
}
