package com.magicvector.manager.control

import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.dto.response.ControlAgentLogResponse
import com.magicvector.MainApplication
import com.magicvector.manager.db.VectorDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ControlAgentLogManager {
    private val api = MainApplication.getApiRequestImplInstance()
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
        api.getControlAgentLogs(
            userId = userId,
            agentId = agentId,
            page = page,
            size = size,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<ControlAgentLogResponse>> {
                override fun onResponse(response: BaseResponse<ControlAgentLogResponse>?) {
                    val list = response?.data?.logs.orEmpty().map {
                        ControlAgentLogEntity(
                            id = it.id ?: 0L,
                            userId = it.userId ?: 0L,
                            agentId = it.agentId ?: 0L,
                            logTime = it.logTime ?: System.currentTimeMillis(),
                            logContent = it.logContent.orEmpty()
                        )
                    }
                    ioScope.launch {
                        if (list.isNotEmpty()) {
                            dao.upsertBatch(list)
                        }
                    }
                    onSuccess.invoke(list)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
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
