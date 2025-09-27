package com.data.domain.vo.chat

import com.data.domain.constant.chat.MessageTypeEnum
import com.data.domain.constant.chat.SendMessageTypeEnum
import java.io.Serializable

class ChatItemVo : Serializable{
    // 图片资源
    var imgUrl: String = ""
    // 消息概览
    var content: String = ""
    // 时间
    var time: String? = null
    // 是否已读
    var isRead: Boolean = false
    // 发送消息类型
    var viewType: Int = SendMessageTypeEnum.VIEW_TYPE_SENDER.value
    // 消息类型
    var messageType: Int = MessageTypeEnum.TEXT.value
}