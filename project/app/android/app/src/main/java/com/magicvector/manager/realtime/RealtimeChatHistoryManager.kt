package com.magicvector.manager.realtime


import android.util.Log
import com.magicvector.manager.event.chat.ChatEventManager
import com.magicvector.utils.sort.PageDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 历史消息管理器
 * 负责消息的加载、刷新、缓存清理
 */
class RealtimeChatHistoryManager(
    private val coroutineScope: CoroutineScope,
    private val chatEventManager: ChatEventManager?
) {
    companion object {
        const val TAG = "RealtimeChatHistoryManager"
    }

    /**
     * 刷新消息（全量加载）
     */
    fun refreshMessages() {
        coroutineScope.launch {
            chatEventManager?.onHttpFullLoad()
        }
    }

    /**
     * 加载更多历史消息
     */
    fun loadMoreMessages() {
        coroutineScope.launch {
            try {
                chatEventManager?.onHttpPageLoad(PageDirection.UP, 20)
            } catch (e: Exception) {
                Log.e(TAG, "loadMoreMessages failed", e)
            }
        }
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        chatEventManager?.clearCache()
    }
}