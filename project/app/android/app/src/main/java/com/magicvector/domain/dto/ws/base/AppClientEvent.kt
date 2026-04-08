package com.magicvector.domain.dto.ws.base

/**
 * App 客户端发送的事件
 */
class AppClientEvent(
    override val channel: String,
    override val event: String,
    override val data: Map<String, String> = emptyMap()
) : ClientEvent(channel, event, data)