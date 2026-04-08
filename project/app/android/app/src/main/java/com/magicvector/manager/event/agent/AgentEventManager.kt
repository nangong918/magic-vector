package com.magicvector.manager.event.agent

import com.magicvector.MainApplication
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.manager.event.AbstractEventManager
import com.magicvector.utils.sort.PageDirection
import com.magicvector.utils.sort.SortItem
import com.magicvector.utils.sort.SortMode

class AgentEventManager() : AbstractEventManager<AgentChatModel>() {

    companion object {
        private const val TAG = "AgentEventManager"
        private val api = MainApplication.getRemoteApiSource()
        private val local = MainApplication.getAgentLocalSource()
        const val FULL_LIMIT = 20
    }

    override suspend fun loadFromRemoteFull(sortMode: SortMode): List<AgentChatModel> {
        val userId = MainApplication.getUserId()
        if (userId.isEmpty()) return emptyList()
        return api.getAgentListFull(userId)
    }

    override suspend fun loadFromRemotePage(
        sortMode: SortMode,
        direction: PageDirection,
        limit: Int,
        cursor: SortItem?
    ): List<AgentChatModel> {
        val userId = MainApplication.getUserId()
        if (userId.isEmpty()) return emptyList()

        val sortField = when (sortMode) {
            is SortMode.TimestampSort -> "lastChatTime"
            is SortMode.UidSort -> "agentId"
            is SortMode.StringSort -> "name"
        }

        val sortOrder = when (sortMode) {
            is SortMode.TimestampSort -> if (sortMode.isDesc) "DESC" else "ASC"
            is SortMode.UidSort -> if (sortMode.isDesc) "DESC" else "ASC"
            is SortMode.StringSort -> if (sortMode.isDesc) "DESC" else "ASC"
        }

        val pageDirectionStr = when (direction) {
            PageDirection.UP -> "before"
            PageDirection.DOWN -> "after"
        }

        val cursorValue = when (sortMode) {
            is SortMode.TimestampSort -> cursor?.getTimestamp()?.toString() ?: ""
            is SortMode.UidSort -> cursor?.getUid()?.toString() ?: ""
            is SortMode.StringSort -> cursor?.getStringIndex() ?: ""
        }

        return api.getAgentListPage(
            userId = userId,
            sortField = sortField,
            sortOrder = sortOrder,
            pageDirection = pageDirectionStr,
            cursor = cursorValue,
            limit = limit
        )
    }

    override suspend fun loadFromLocalFull(sortMode: SortMode): List<AgentChatModel> {
        val userId = MainApplication.getUserId().toLongOrNull() ?: return emptyList()

        return when (sortMode) {
            is SortMode.TimestampSort -> {
                local.queryFull(
                    userId = userId,
                    orderBy = "last_chat_time",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    limit = FULL_LIMIT
                )
            }
            is SortMode.UidSort -> {
                local.queryFull(
                    userId = userId,
                    orderBy = "agent_id",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    limit = FULL_LIMIT
                )
            }
            is SortMode.StringSort -> {
                local.queryFullByName(
                    userId = userId,
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    limit = FULL_LIMIT
                )
            }
        }
    }

    override suspend fun loadFromLocalPage(
        sortMode: SortMode,
        direction: PageDirection,
        limit: Int,
        cursor: SortItem?
    ): List<AgentChatModel> {
        val userId = MainApplication.getUserId().toLongOrNull() ?: return emptyList()

        // 获取游标值
        val cursorValue = when (sortMode) {
            is SortMode.TimestampSort -> cursor?.getTimestamp() ?: return emptyList()
            is SortMode.UidSort -> cursor?.getUid() ?: return emptyList()
            is SortMode.StringSort -> cursor?.getStringIndex() ?: return emptyList()
        }

        // 分页方向转换
        val pageDirection = when (direction) {
            PageDirection.UP -> "before"
            PageDirection.DOWN -> "after"
        }

        return when (sortMode) {
            is SortMode.TimestampSort -> {
                local.queryPage(
                    userId = userId,
                    orderBy = "last_chat_time",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    pageDirection = pageDirection,
                    cursor = cursorValue as Long,
                    limit = limit
                )
            }
            is SortMode.UidSort -> {
                local.queryPage(
                    userId = userId,
                    orderBy = "agent_id",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    pageDirection = pageDirection,
                    cursor = cursorValue as Long,
                    limit = limit
                )
            }
            is SortMode.StringSort -> {
                local.queryPageByName(
                    userId = userId,
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    pageDirection = pageDirection,
                    cursor = cursorValue as String,
                    limit = limit
                )
            }
        }
    }
}