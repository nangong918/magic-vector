package com.data.domain.ao.chat

import com.core.baseutil.sort.SortItem
import com.data.domain.vo.chat.ChatItemVo

class ChatItemAo : SortItem{

    // vo
    var vo : ChatItemVo = ChatItemVo()

    // data
    // senderId = agentId
    var senderId : String? = null
    // receiverId = userId
    var receiverId : String? = null
    var messageId : String? = null
    var timestamp : Long = 0L

    // 排序方法
    override fun getIndex(): Long {
        return timestamp
    }
}