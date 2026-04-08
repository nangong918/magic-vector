package com.magicvector.domain.vo.message

import com.magicvector.domain.constant.chat.MessageTypeEnum
import com.magicvector.domain.constant.chat.RoleTypeEnum

/**
 * 聊天项vo
 */
data class ChatMessageVO(
    var briefMessageVo: ChatBriefMessageVO,
    // 图片资源
    var imgUrl: String,
    // 消息类型
    var messageType: Int = MessageTypeEnum.TEXT.value
//    // 是否已读
//    var isRead: Boolean = false
)

/**
 * 聊天简略消息vo
 * 详情vo:
 * @see ChatMessageVO
 */
data class ChatBriefMessageVO(
    // 内容
    val content: String,
    // 时间(展示用)
    val chatTime: String,
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    val role: Int = RoleTypeEnum.AGENT.value
)