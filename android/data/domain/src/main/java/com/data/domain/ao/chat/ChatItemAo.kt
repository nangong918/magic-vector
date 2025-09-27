package com.data.domain.ao.chat

import com.data.domain.vo.chat.ChatItemVo

class ChatItemAo {

    // vo
    var vo : ChatItemVo = ChatItemVo()

    // data
    var senderId : String? = null
    var receiverId : String? = null
    var messageId : String? = null
    var timestamp : Long = 0L
}