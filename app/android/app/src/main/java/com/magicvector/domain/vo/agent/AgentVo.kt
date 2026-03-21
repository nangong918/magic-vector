package com.magicvector.domain.vo.agent

/**
 * Agent Item vo
 */
data class AgentVo(
    // 名称/描述一般不修改 → 不可变val
    val name: String,
    val description: String,
    // 头像URL可能动态修改 → 可变var + 可空
    var avatarUrl: String? = null
)