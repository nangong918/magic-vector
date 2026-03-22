package com.magicvector.domain.vo.message

import com.data.domain.constant.chat.MessageTypeEnum

/**
 * 聊天项vo
 */
data class ChatMessageVo(
    var briefMessageVo: ChatBriefMessageVo,
    // 图片资源
    var imgUrl: String,
    // 消息类型
    var messageType: Int = MessageTypeEnum.TEXT.value
//    // 是否已读
//    var isRead: Boolean = false
)