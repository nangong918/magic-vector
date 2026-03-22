package com.magicvector.domain.model.chat

import com.magicvector.domain.vo.message.ChatMessageVo
import com.magicvector.utils.sort.SortItem

/**
 * 聊天消息的model
 */
data class ChatMessageModel(
    // vo
    val chatMessageVo : ChatMessageVo,
    // data
    var agentId : Long,
    var userId : Long,
    var messageId : Long,
    var timestamp : Long
) : SortItem {
    // 排序方法
    override fun getUid(): Long {
        return messageId
    }
    override fun getTimestamp(): Long {
        return timestamp
    }
}