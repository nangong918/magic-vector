package com.magicvector.domain.ws

/**
 * App 客户端发送的事件
 */
class AppClientEvent(
    override val channel: String,
    override val data: Map<String, String> = emptyMap()
) : ClientEvent(channel, data)