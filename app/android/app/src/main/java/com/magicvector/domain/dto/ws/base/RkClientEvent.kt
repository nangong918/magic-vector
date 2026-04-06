package com.magicvector.domain.dto.ws.base


/**
 * RK 客户端发送的事件
 */
class RkClientEvent(
    override val channel: String,
    override val event: String,
    override val data: Map<String, String> = emptyMap()
) : ClientEvent(channel, event, data)