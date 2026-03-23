package com.magicvector.domain.dto.ws.response

import com.data.domain.constant.chat.RoleTypeEnum

/**
 * WebSocket实时聊天文本响应DTO
 */
data class WsChatTextResponse(
    // agentId也相当于会话Id
    var agentId: String,
    // userId
    var userId: String,
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    var role: Int = RoleTypeEnum.AGENT.value,
    // 内容
    var content: String = "",
    // 消息Id
    var messageId: String,
    // timestamp: 消息发送的时间 用于排序
    var timestamp: Long,
    // date time：消息发送时间 用于展示
    var chatTime: String = ""
)
