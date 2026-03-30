package com.magicvector.domain.ws


/**
 * RK 客户端发送的事件
 */
class RkClientEvent(
    override val channel: String,
    override val data: Map<String, String> = emptyMap()
) : ClientEvent(channel, data)