package com.magicvector.domain.dto.ws.base


import java.util.UUID

/**
 * 队列消息包装
 */
data class QueueMessage<T>(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val payload: T
)