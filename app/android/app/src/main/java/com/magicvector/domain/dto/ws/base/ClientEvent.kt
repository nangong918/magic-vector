package com.magicvector.domain.dto.ws.base


/**
 * 客户端发送事件基类
 */
open class ClientEvent(
    /**
     * 事件路由，见 WsChannel 枚举
     * @see com.magicvector.domain.constant.ws.WsChannel
     */
    open val channel: String,

    /**
     * 业务数据，key-value 格式
     */
    open val data: Map<String, String> = emptyMap()
)