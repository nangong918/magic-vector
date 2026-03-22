package com.magicvector.manager.event.chat

import com.magicvector.MainApplication
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.manager.event.AbstractEventManager
import com.magicvector.utils.sort.PageDirection
import com.magicvector.utils.sort.SortItem
import com.magicvector.utils.sort.SortMode

class ChatEventManager(
    private val agentId: Long
) : AbstractEventManager<ChatMessageModel>() {

    companion object {
        private const val TAG = "ChatEventManager"
        private val api = MainApplication.getRemoteApiSource()
        private val local = MainApplication.getChatLocalSource()
        const val FULL_LIMIT = 20
    }

    override suspend fun loadFromRemoteFull(sortMode: SortMode): List<ChatMessageModel> {
        val userId = MainApplication.getUserId()
        if (userId.isEmpty()) return emptyList()

        return api.getChatListFull(agentId.toString(), userId)
    }

    override suspend fun loadFromRemotePage(
        sortMode: SortMode,
        direction: PageDirection,
        limit: Int,
        cursor: SortItem?
    ): List<ChatMessageModel> {
        val userId = MainApplication.getUserId()
        if (userId.isEmpty()) return emptyList()

        val sortField = when (sortMode) {
            is SortMode.TimestampSort -> "timestamp"
            is SortMode.UidSort -> "messageId"
            else -> "timestamp"
        }

        val sortOrder = when (sortMode) {
            is SortMode.TimestampSort -> if (sortMode.isDesc) "DESC" else "ASC"
            is SortMode.UidSort -> if (sortMode.isDesc) "DESC" else "ASC"
            else -> "DESC"
        }

        val pageDirectionStr = when (direction) {
            PageDirection.UP -> "before"
            PageDirection.DOWN -> "after"
        }

        val cursorValue = when (sortMode) {
            is SortMode.TimestampSort -> cursor?.getTimestamp()?.toString() ?: ""
            is SortMode.UidSort -> cursor?.getUid()?.toString() ?: ""
            else -> ""
        }

        return api.getChatListPage(
            agentId = agentId.toString(),
            userId = userId,
            sortField = sortField,
            sortOrder = sortOrder,
            pageDirection = pageDirectionStr,
            cursor = cursorValue,
            limit = limit
        )
    }

    override suspend fun loadFromLocalFull(sortMode: SortMode): List<ChatMessageModel> {
        val userId = MainApplication.getUserId().toLongOrNull() ?: return emptyList()

        return when (sortMode) {
            is SortMode.TimestampSort -> {
                local.queryFull(
                    agentId = agentId,
                    userId = userId,
                    orderBy = "timestamp",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    limit = FULL_LIMIT
                )
            }
            is SortMode.UidSort -> {
                local.queryFull(
                    agentId = agentId,
                    userId = userId,
                    orderBy = "message_id",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    limit = FULL_LIMIT
                )
            }
            else -> emptyList()
        }
    }

    override suspend fun loadFromLocalPage(
        sortMode: SortMode,
        direction: PageDirection,
        limit: Int,
        cursor: SortItem?
    ): List<ChatMessageModel> {
        val userId = MainApplication.getUserId().toLongOrNull() ?: return emptyList()

        // 获取游标值
        val cursorValue = when (sortMode) {
            is SortMode.TimestampSort -> cursor?.getTimestamp() ?: return emptyList()
            is SortMode.UidSort -> cursor?.getUid() ?: return emptyList()
            else -> return emptyList()
        }

        // 分页方向转换
        val pageDirection = when (direction) {
            PageDirection.UP -> "before"
            PageDirection.DOWN -> "after"
        }

        return when (sortMode) {
            is SortMode.TimestampSort -> {
                local.queryPage(
                    agentId = agentId,
                    userId = userId,
                    orderBy = "timestamp",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    pageDirection = pageDirection,
                    cursor = cursorValue,
                    limit = limit
                )
            }
            is SortMode.UidSort -> {
                local.queryPage(
                    agentId = agentId,
                    userId = userId,
                    orderBy = "message_id",
                    sortOrder = if (sortMode.isDesc) "DESC" else "ASC",
                    pageDirection = pageDirection,
                    cursor = cursorValue,
                    limit = limit
                )
            }
            else -> emptyList()
        }
    }
}