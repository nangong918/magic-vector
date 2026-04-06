package com.magicvector.domain.dto.ws.base


/**
 * 服务端发送的事件
 */
data class ServerEvent(
    /**
     * 事件路由，见 WsChannel 枚举
     * @see com.magicvector.domain.constant.ws.WsEvent
     */
    val channel: String,
    /**
     * 事件ID，用于标识事件
     * @see com.magicvector.domain.constant.ws.WsEvent
     */
    val eventId: String,
    /**
     * 业务数据，key-value 格式
     */
    val data: Map<String, String> = emptyMap()
)