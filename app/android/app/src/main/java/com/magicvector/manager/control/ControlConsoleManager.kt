package com.magicvector.manager.control

import android.util.Log
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.BaseConstant
import com.magicvector.domain.dto.http.request.ControlCommandRequest
import com.magicvector.domain.dto.http.response.ControlCommandResponse
import com.magicvector.domain.dto.http.response.ControlStatusResponse
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 控制台网络管理器（HTTP + WS）。
 * 说明：RTMP 拉流与推流链路暂未在本类实现，后续与音视频模块集成。
 */
class ControlConsoleManager {

    companion object {
        private const val TAG = "ControlConsoleManager"
        private const val MAX_RETRY_COUNT = 5
        private const val RETRY_DELAY_MS = 2_000L
    }

    private val gson = MainApplication.GSON
    private val api = MainApplication.getRemoteApiSource()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reconnectEnabled = AtomicBoolean(false)
    private val wsClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val _wsState = MutableStateFlow(ControlWsState())
    val wsState: StateFlow<ControlWsState> = _wsState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var deviceId: String = ""
    private var userId: String = ""
    private var onWsPayload: ((Map<String, String>) -> Unit)? = null

    fun connectControlWs(
        userId: String,
        deviceId: String,
        onWsPayload: (Map<String, String>) -> Unit
    ) {
        if (userId.isBlank() || deviceId.isBlank()) {
            return
        }
        if (this.userId == userId && this.deviceId == deviceId && _wsState.value.connected) {
            this.onWsPayload = onWsPayload
            return
        }
        this.userId = userId
        this.deviceId = deviceId
        this.onWsPayload = onWsPayload
        reconnectEnabled.set(true)
        openWebSocket()
    }

    fun disconnectControlWs() {
        reconnectEnabled.set(false)
        webSocket?.close(1000, "control ws close")
        webSocket = null
        _wsState.value = ControlWsState()
    }

    fun queryControlStatus(
        deviceId: String,
        onSuccess: (ControlStatusResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        api.getControlStatus(
            deviceId = deviceId,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<ControlStatusResponse>> {
                override fun onResponse(response: BaseResponse<ControlStatusResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }

    fun sendControlCommand(
        request: ControlCommandRequest,
        onSuccess: (ControlCommandResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        // 优先走 WS 低时延通道；如果当前未连接则回落 HTTP。
        val sentByWs = sendWsCommand(request)
        if (sentByWs) {
            onSuccess.invoke(
                ControlCommandResponse().apply {
                    accepted = true
                    traceId = "ws-local"
                    message = "command sent by ws"
                }
            )
            return
        }
        api.sendControlCommand(
            request = request,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<ControlCommandResponse>> {
                override fun onResponse(response: BaseResponse<ControlCommandResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }

    fun sendHeartbeat() {
        val body = mapOf(
            "type" to "HEARTBEAT",
            "deviceId" to deviceId,
            "userId" to userId,
            "timestamp" to System.currentTimeMillis().toString()
        )
        webSocket?.send(gson.toJson(body))
    }

    private fun sendWsCommand(request: ControlCommandRequest): Boolean {
        val ws = webSocket ?: return false
        if (!_wsState.value.connected) {
            return false
        }
        val data = mapOf(
            "type" to "COMMAND",
            "deviceId" to request.deviceId.orEmpty(),
            "userId" to request.userId.orEmpty(),
            "transport" to request.transport.orEmpty(),
            "commandType" to request.commandType.orEmpty(),
            "sequence" to (request.sequence ?: 0L).toString(),
            "timestamp" to (request.timestamp ?: System.currentTimeMillis()).toString(),
            "payloadJson" to request.payloadJson.orEmpty()
        )
        return ws.send(gson.toJson(data))
    }

    private fun openWebSocket() {
        val previous = webSocket
        if (previous != null) {
            runCatching { previous.close(1000, "replace control ws") }
        }
        _wsState.value = _wsState.value.copy(connected = false, reconnecting = true)
        val wsUrl = buildControlWsUrl(userId = userId, deviceId = deviceId)
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        val nextSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                if (this@ControlConsoleManager.webSocket !== webSocket) {
                    return
                }
                _wsState.value = ControlWsState(connected = true, reconnecting = false, retryCount = 0)
                sendHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (this@ControlConsoleManager.webSocket !== webSocket) {
                    return
                }
                runCatching {
                    gson.fromJson<Map<String, String>>(
                        text,
                        object : TypeToken<Map<String, String>>() {}.type
                    )
                }.onSuccess {
                    onWsPayload?.invoke(it ?: emptyMap())
                }.onFailure {
                    Log.w(TAG, "onMessage parse failed", it)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (this@ControlConsoleManager.webSocket !== webSocket) {
                    return
                }
                Log.d(TAG, "binary payload size=${bytes.size}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                if (this@ControlConsoleManager.webSocket !== webSocket) {
                    return
                }
                Log.e(TAG, "control ws onFailure", t)
                _wsState.value = _wsState.value.copy(
                    connected = false,
                    reconnecting = true,
                    lastError = t.message
                )
                if (reconnectEnabled.get()) {
                    tryReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (this@ControlConsoleManager.webSocket !== webSocket) {
                    return
                }
                _wsState.value = _wsState.value.copy(connected = false, reconnecting = false)
            }
        })
        webSocket = nextSocket
    }

    private fun tryReconnect() {
        if (!reconnectEnabled.get()) {
            return
        }
        val retryCount = _wsState.value.retryCount
        if (retryCount >= MAX_RETRY_COUNT) {
            _wsState.value = _wsState.value.copy(reconnecting = false)
            return
        }
        managerScope.launch {
            delay(RETRY_DELAY_MS)
            _wsState.value = _wsState.value.copy(retryCount = retryCount + 1)
            openWebSocket()
        }
    }

    private fun buildControlWsUrl(userId: String, deviceId: String): String {
        val wsBase = BaseConstant.ConstantUrl.LOCAL_WS_URL
        val path = BaseConstant.WSConstantUrl.CONTROL_CONSOLE_URL
        return "$wsBase$path?clientType=app&userId=$userId&deviceId=$deviceId"
    }
}

data class ControlWsState(
    val connected: Boolean = false,
    val reconnecting: Boolean = false,
    val retryCount: Int = 0,
    val lastError: String? = null
)
