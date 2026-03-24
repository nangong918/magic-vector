package com.magicvector.domain.model.chat

import com.magicvector.domain.vo.message.ChatMessageVO
import com.magicvector.utils.sort.SortItem

/**
 * 聊天消息的model
 */
class ChatMessageModel(
    // vo
    val chatMessageVo: ChatMessageVO,
    // data
    var agentId: Long,
    var userId: Long,
    var messageId: Long,
    @JvmField var timestamp: Long  // 添加 @JvmField
) : SortItem {
    // 排序方法
    override fun getUid(): Long {
        return messageId
    }

    override fun getTimestamp(): Long {
        return timestamp
    }
}