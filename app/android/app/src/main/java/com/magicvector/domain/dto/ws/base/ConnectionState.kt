package com.magicvector.domain.dto.ws.base


import com.magicvector.domain.constant.ws.ClientType
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * WebSocket 连接状态
 */
class ConnectionState {
    var isConnected = false
    var sessionId: String? = null
    var clientType: ClientType? = null
    var clientId: String? = null           // userId 或 deviceId
    var lastHeartbeat: Long = 0L

    val sendQueue: BlockingQueue<ClientEvent> = LinkedBlockingQueue(32)
    val receiveQueue: BlockingQueue<ServerEvent> = LinkedBlockingQueue(32)
}