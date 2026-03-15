package com.magicvector.manager.control

import com.magicvector.MainApplication
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.entity.ControlAgentLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ControlAgentLogManager {
    private val api = MainApplication.getRemoteApiSource()
    private val dao = VectorDatabase.getInstance(MainApplication.getApp()).controlAgentLogDao()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun fetchOnlineLogs(
        userId: String,
        agentId: String?,
        page: Int,
        size: Int,
        onSuccess: (List<ControlAgentLogEntity>) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        ioScope.launch {
            runCatching {
                api.getControlAgentLogs(
                    userId = userId,
                    agentId = agentId,
                    page = page,
                    size = size
                )
            }.onSuccess { response ->
                val list = response.logs.orEmpty().map {
                    ControlAgentLogEntity(
                        id = it.id ?: 0L,
                        userId = it.userId ?: 0L,
                        agentId = it.agentId ?: 0L,
                        logTime = it.logTime ?: System.currentTimeMillis(),
                        logContent = it.logContent.orEmpty()
                    )
                }
                if (list.isNotEmpty()) {
                    dao.upsertBatch(list)
                }
                onSuccess.invoke(list)
            }.onFailure {
                onError.invoke(it)
            }
        }
    }

    fun appendLocalLogs(list: List<ControlAgentLogEntity>) {
        ioScope.launch {
            if (list.isNotEmpty()) {
                dao.upsertBatch(list)
            }
        }
    }

    fun queryLocalLogs(
        userId: Long,
        agentId: Long?,
        limit: Int,
        callback: (List<ControlAgentLogEntity>) -> Unit
    ) {
        ioScope.launch {
            val result = if (agentId != null && agentId > 0) {
                dao.queryByAgentIdLimit(userId, agentId, limit)
            } else {
                dao.queryByUserLimit(userId, limit)
            }
            callback.invoke(result)
        }
    }
}
