package com.magicvector.domain.dto.http.response

import com.magicvector.domain.constant.chat.RoleTypeEnum


data class AgentListResponse (
    val agentList: List<AgentChatDto> = emptyList()
)

data class AgentResponse(
    val agent : AgentChatDto,
)

// 后端返回的数据类型
data class AgentChatDto(
    // AgentChatModel
    var agentId: Long = 0L,
    var userId: Long = 0L,
    var lastChatTime: Long = 0L,
    var updatedAt: Long = 0L,

    // AgentChatVo
    val unreadCount: Int = 0,

    // AgentVo
    // 名称/描述一般不修改 → 不可变val
    val name: String,
    val description: String,
    // 头像URL可能动态修改 → 可变var + 可空
    var avatarUrl: String? = null,

    // ChatMessageVo
    // 内容
    val content: String,
    // 时间(展示用)
    val chatTime: String,
    // 发送方: 0: agent, 1: user (相当于isUser ? 0 : 1)
    val role: Int = RoleTypeEnum.AGENT.value
)









