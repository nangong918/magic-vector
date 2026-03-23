package com.magicvector.manager.realtime


import android.util.Log
import com.magicvector.repository.api.config.ApiUrlConfig
import com.data.domain.constant.BaseConstant
import com.magicvector.MainApplication
import com.magicvector.manager.ws.WsManager
import com.magicvector.utils.chat.RealtimeChatWsClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket 连接管理器
 * 负责 WS 连接的建立、维护、重连
 */
class RealtimeChatWebSocketManager(
    private val eventFlow: RealtimeChatEventFlow,
    private val messageHandler: RealtimeChatMessageHandler
) {
    companion object {
        const val TAG = "RealtimeChatWebSocketManager"
        val GSON = MainApplication.GSON
    }

    private var realtimeChatWsClient: RealtimeChatWsClient? = null
    private var userId: Long? = null
    private var agentId: Long? = null
    private val manualWsClosing = AtomicBoolean(false)

    /**
     * 确保用户级别的 WebSocket 连接
     */
    fun ensureUserConnection(userId: String): Boolean {
        if (userId.isBlank()) {
            Log.w(TAG, "ensureUserConnection: userId is blank")
            return false
        }
        this.userId = userId.toLongOrNull()
        realtimeChatWsClient = initRealtimeChatWsClient()

        if (!MainApplication.getNetworkManager().refreshNetworkState().isNetworkOnline) {
            eventFlow.updateRealtimeState(RealtimeChatState.Disconnected)
            Log.i(TAG, "ensureUserConnection: network offline, skip ws connect")
            return false
        }
        startRealtimeWs()
        return true
    }

    /**
     * 绑定 Agent 频道
     */
    fun bindChannel(agentId: Long) {
        this.agentId = agentId
        realtimeChatWsClient?.let { client ->
            if (eventFlow.realtimeState.value == RealtimeChatState.InitializedConnected ||
                eventFlow.realtimeState.value == RealtimeChatState.Receiving ||
                eventFlow.realtimeState.value == RealtimeChatState.RecordingAndSending
            ) {
                WsManager.sendBindChannelInfo(agentId.toString(), client)
            }
        }
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(message: String) {
        val dataMap = mapOf(
            com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.TYPE to com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            com.data.domain.constant.chat.RealtimeRequestDataTypeEnum.DATA to message
        )
        realtimeChatWsClient?.sendMessage(dataMap)
    }

    /**
     * 发送系统消息（MCP 开关等）
     */
    fun sendSystemMessage(dataMap: Map<String, String>) {
        realtimeChatWsClient?.sendMessage(dataMap)
    }

    /**
     * 尝试重连
     */
    fun reconnectIfNeeded() {
        val userId = userId ?: return
        if (!MainApplication.getNetworkManager().refreshNetworkState().isNetworkOnline) return
        realtimeChatWsClient = initRealtimeChatWsClient()
        startRealtimeWs(forceReconnect = true)
    }

    /**
     * 释放资源
     */
    fun release() {
        realtimeChatWsClient?.let {
            manualWsClosing.set(true)
            it.close()
            realtimeChatWsClient = null
            MainApplication.getNetworkManager().onWebSocketDisconnected(shouldReconnect = false)
        }
    }

    // ========== 私有方法 ==========

    private fun initRealtimeChatWsClient(): RealtimeChatWsClient {
        return realtimeChatWsClient ?: synchronized(this) {
            realtimeChatWsClient ?: RealtimeChatWsClient(
                GSON,
                ApiUrlConfig.getWsMainUrl() + BaseConstant.WSConstantUrl.AGENT_REALTIME_CHAT_URL
            ).also { realtimeChatWsClient = it }
        }
    }

    private fun startRealtimeWs(forceReconnect: Boolean = false) {
        if (!forceReconnect && (eventFlow.realtimeState.value == RealtimeChatState.InitializedConnected ||
                    eventFlow.realtimeState.value == RealtimeChatState.Receiving ||
                    eventFlow.realtimeState.value == RealtimeChatState.RecordingAndSending
                    )) {
            realtimeChatWsClient?.let { client ->
                userId?.let { WsManager.sendConnectInfo(it.toString(), client) }
                agentId?.let { WsManager.sendBindChannelInfo(it.toString(), client) }
            }
            return
        }

        realtimeChatWsClient?.let { client ->
            manualWsClosing.set(false)
            val listener = createWebSocketListener(client)
            if (forceReconnect) {
                client.reconnect(listener)
            } else {
                client.start(listener)
            }
        }
    }

    private fun createWebSocketListener(client: RealtimeChatWsClient): WebSocketListener {
        return object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                eventFlow.updateRealtimeState(RealtimeChatState.Disconnected)
                val shouldReconnect = !manualWsClosing.getAndSet(false)
                MainApplication.getNetworkManager().onWebSocketDisconnected(shouldReconnect)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "realtimeChatWsClient::onFailure: ${t.message}")
                eventFlow.updateRealtimeState(RealtimeChatState.Error(t.message ?: "-"))
                val shouldReconnect = !manualWsClosing.getAndSet(false)
                MainApplication.getNetworkManager().onWebSocketDisconnected(shouldReconnect)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                eventFlow.updateRealtimeState(RealtimeChatState.Receiving)
                messageHandler.handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                eventFlow.updateRealtimeState(RealtimeChatState.Receiving)
                Log.i(TAG, "收到字节信息::长度: ${bytes.size}")
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                eventFlow.updateRealtimeState(RealtimeChatState.InitializedConnected)
                MainApplication.getNetworkManager().onWebSocketConnected()
                userId?.let { WsManager.sendConnectInfo(it.toString(), client) }
                agentId?.let { WsManager.sendBindChannelInfo(it.toString(), client) }
            }
        }
    }
}