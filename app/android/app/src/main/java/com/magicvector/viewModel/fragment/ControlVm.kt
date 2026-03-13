package com.magicvector.viewModel.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.dto.request.ControlCommandRequest
import com.magicvector.MainApplication
import com.magicvector.manager.control.ControlCommandController
import com.magicvector.manager.control.ControlAgentLogEntity
import com.magicvector.manager.control.ControlWsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ControlVm : ViewModel() {

    /** MVI: UI 渲染状态。 */
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    /** MVI: 非 UI 关键业务状态。 */
    private val _dataState = MutableStateFlow(ControlDataState())
    val dataState: StateFlow<ControlDataState> = _dataState.asStateFlow()

    /** MVI: 一次性副作用（Toast/外部启动器）。 */
    private val _effect = Channel<ControlEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val manager = MainApplication.getControlConsoleManager()
    private val logManager = MainApplication.getControlAgentLogManager()
    private val commandController = ControlCommandController()
    private var recordTickerJob: Job? = null
    private var initialized = false

    fun processIntent(intent: ControlIntent) {
        when (intent) {
            ControlIntent.Initialize -> initialize()
            is ControlIntent.UpdateDeviceId -> _uiState.update { it.copy(deviceId = intent.deviceId) }
            is ControlIntent.SwitchPlatform -> {
                _uiState.update {
                    it.copy(
                        platform = intent.platform,
                        streamEnabled = intent.platform != ControlPlatform.OFFLINE_BLE
                    )
                }
            }
            is ControlIntent.SwitchStreamSource -> _uiState.update { it.copy(streamSource = intent.source) }
            is ControlIntent.SwitchTestMode -> _uiState.update { it.copy(testMode = intent.mode) }
            is ControlIntent.UpdateTestRtmpUrl -> _uiState.update { it.copy(testRtmpUrl = intent.url) }
            ControlIntent.StartTestStream -> onStartTestStream()
            ControlIntent.StopTestStream -> onStopTestStream()
            is ControlIntent.LeftJoystickDrag -> sendJoystickCommand("leftJoystick", intent.x, intent.y)
            is ControlIntent.RightJoystickDrag -> sendJoystickCommand("rightJoystick", intent.x, intent.y)
            ControlIntent.ToggleRecording -> toggleRecording()
            ControlIntent.FetchControlStatus -> fetchControlStatus()
            ControlIntent.ReconnectControlWs -> reconnectControlWs()
            ControlIntent.SendQuickCommandForward -> sendQuickButtonCommand("FORWARD")
            ControlIntent.SendQuickCommandStop -> sendQuickButtonCommand("STOP")
            ControlIntent.FetchControlAgentLogs -> fetchControlAgentLogs()
        }
    }

    private fun initialize() {
        if (initialized) {
            return
        }
        initialized = true
        val userId = MainApplication.getUserId()
        if (userId.isBlank()) {
            sendEffect(ControlEffect.ShowToast("用户未登录，控制台不可用"))
            return
        }
        _dataState.update { it.copy(userId = userId) }
        observeNetworkState()
        observeControlWsState()
        reconnectControlWs()
        fetchControlStatus()
        fetchControlAgentLogs()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            MainApplication.getNetworkManager().state.collect { state ->
                _uiState.update {
                    it.copy(
                        appToSpringConnected = state.isOnlineAndWsReady,
                        networkOnline = state.isNetworkOnline
                    )
                }
            }
        }
    }

    private fun observeControlWsState() {
        viewModelScope.launch {
            manager.wsState.collect { wsState: ControlWsState ->
                _uiState.update {
                    it.copy(
                        controlWsConnected = wsState.connected,
                        wsReconnecting = wsState.reconnecting,
                        wsRetryCount = wsState.retryCount,
                        wsErrorMessage = wsState.lastError
                    )
                }
            }
        }
    }

    private fun reconnectControlWs() {
        val userId = _dataState.value.userId
        val deviceId = _uiState.value.deviceId
        if (userId.isBlank() || deviceId.isBlank()) {
            return
        }
        manager.connectControlWs(
            userId = userId,
            deviceId = deviceId
        ) { payload ->
            val type = payload["type"].orEmpty()
            if (type == "STATUS_SYNC") {
                _uiState.update {
                    it.copy(
                        rkToSpringConnected = payload["rkToSpringConnected"] == "true",
                        appToRkWifiConnected = payload["appToRkWifiConnected"] == "true",
                        appToRkBleConnected = payload["appToRkBleConnected"] == "true",
                        rkAgentMode = payload["rkAgentMode"] ?: it.rkAgentMode
                    )
                }
                val logText = payload["agentJsonLog"]
                if (!logText.isNullOrBlank()) {
                    appendRealtimeLog(logText)
                }
            }
            if (type == "AGENT_JSON_LOG") {
                val logText = payload["logContent"].orEmpty()
                if (logText.isNotBlank()) {
                    appendRealtimeLog(logText)
                }
            }
        }
    }

    private fun fetchControlStatus() {
        manager.queryControlStatus(
            deviceId = _uiState.value.deviceId,
            onSuccess = { status ->
                if (status == null) {
                    return@queryControlStatus
                }
                _uiState.update {
                    it.copy(
                        appToSpringConnected = status.appToSpringConnected == true,
                        rkToSpringConnected = status.rkToSpringConnected == true,
                        appToRkWifiConnected = status.appToRkWifiConnected == true,
                        appToRkBleConnected = status.appToRkBleConnected == true,
                        rkAgentMode = status.rkAgentMode ?: "TODO_RK_AGENT",
                        lastHeartbeatAt = status.lastHeartbeatAt
                    )
                }
            },
            onError = {
                sendEffect(ControlEffect.ShowToast("获取控制状态失败"))
            }
        )
    }

    private fun sendJoystickCommand(key: String, x: Float, y: Float) {
        sendControlCommand(
            commandController.buildJoystickCommand(
                userId = _dataState.value.userId,
                deviceId = _uiState.value.deviceId,
                transport = _uiState.value.platform.transportCode,
                key = key,
                x = x,
                y = y
            )
        )
    }

    private fun sendQuickButtonCommand(action: String) {
        sendControlCommand(
            commandController.buildButtonCommand(
                userId = _dataState.value.userId,
                deviceId = _uiState.value.deviceId,
                transport = _uiState.value.platform.transportCode,
                action = action
            )
        )
    }

    private fun sendControlCommand(request: ControlCommandRequest) {
        manager.sendControlCommand(
            request = request,
            onSuccess = { response ->
                _uiState.update {
                    it.copy(lastCommandTraceId = response?.traceId ?: "local")
                }
                fetchControlAgentLogs()
            },
            onError = {
                sendEffect(ControlEffect.ShowToast("指令发送失败"))
            }
        )
    }

    private fun toggleRecording() {
        val nowRecording = !_uiState.value.recording
        _uiState.update { it.copy(recording = nowRecording) }
        if (!nowRecording) {
            recordTickerJob?.cancel()
            _uiState.update { it.copy(recordSeconds = 0L) }
            sendEffect(ControlEffect.ShowToast("已停止录制（TODO: 录制编码链路待接入）"))
            return
        }
        sendEffect(ControlEffect.ShowToast("开始录制（TODO: H264/UDP 转 MP4）"))
        recordTickerJob?.cancel()
        recordTickerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1_000L)
                _uiState.update { it.copy(recordSeconds = it.recordSeconds + 1) }
            }
        }
    }

    private fun onStartTestStream() {
        if (_uiState.value.testRtmpUrl.isBlank()) {
            sendEffect(ControlEffect.ShowToast("请输入 RTMP 地址"))
            return
        }
        _uiState.update { it.copy(testStreaming = true) }
        sendEffect(ControlEffect.ShowToast("测试流已启动（TODO: RTMP 推拉实现）"))
    }

    private fun onStopTestStream() {
        _uiState.update { it.copy(testStreaming = false) }
    }

    private fun fetchControlAgentLogs() {
        val userId = _dataState.value.userId
        if (userId.isBlank()) {
            return
        }
        val agentId = _uiState.value.rkAgentMode.takeIf { it.all(Char::isDigit) }
        logManager.fetchOnlineLogs(
            userId = userId,
            agentId = agentId,
            page = 1,
            size = 30,
            onSuccess = { logs ->
                _uiState.update {
                    it.copy(
                        agentLogs = logs.map { l -> formatAgentLog(l) }
                    )
                }
            },
            onError = {
                logManager.queryLocalLogs(
                    userId = userId.toLongOrNull() ?: 0L,
                    agentId = agentId?.toLongOrNull(),
                    limit = 30
                ) { local ->
                    _uiState.update {
                        it.copy(agentLogs = local.map { l -> formatAgentLog(l) })
                    }
                }
            }
        )
    }

    private fun appendRealtimeLog(logText: String) {
        val userId = _dataState.value.userId.toLongOrNull() ?: 0L
        val agentId = _uiState.value.rkAgentMode.toLongOrNull() ?: 0L
        val entity = ControlAgentLogEntity(
            id = System.currentTimeMillis(),
            userId = userId,
            agentId = agentId,
            logTime = System.currentTimeMillis(),
            logContent = logText
        )
        logManager.appendLocalLogs(listOf(entity))
        _uiState.update {
            val newList = (listOf(formatAgentLog(entity)) + it.agentLogs).take(50)
            it.copy(agentLogs = newList)
        }
    }

    private fun formatAgentLog(entity: ControlAgentLogEntity): String {
        return "[${entity.logTime}] ${entity.logContent}"
    }

    private fun sendEffect(effect: ControlEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordTickerJob?.cancel()
        manager.disconnectControlWs()
    }
}

/**
 * Control 页面 UI 状态：
 * - 所有会影响 Composable 渲染的字段都放在这里。
 */
data class ControlUiState(
    /** 目标设备 ID（示例值为 mock，后续从设备绑定链路获取）。 */
    val deviceId: String = "rk-default",
    /** 当前平台模式（云控/离线WiFi/离线BLE）。 */
    val platform: ControlPlatform = ControlPlatform.CLOUD,
    /** 视频来源策略（RTMP直连 / UDP经Spring转发）。 */
    val streamSource: StreamSource = StreamSource.RTMP_DIRECT,
    /** App 与 SpringBoot 连接态。 */
    val appToSpringConnected: Boolean = false,
    /** RK 与 SpringBoot 连接态。 */
    val rkToSpringConnected: Boolean = false,
    /** App 与 RK(WiFi) 连接态。 */
    val appToRkWifiConnected: Boolean = false,
    /** App 与 RK(BLE) 连接态。 */
    val appToRkBleConnected: Boolean = false,
    /** RK 当前 Agent 选用状态。 */
    val rkAgentMode: String = "TODO_RK_AGENT",
    /** 网络在线状态。 */
    val networkOnline: Boolean = false,
    /** 控制 WS 连接状态。 */
    val controlWsConnected: Boolean = false,
    /** 控制 WS 是否重连中。 */
    val wsReconnecting: Boolean = false,
    /** 控制 WS 重试次数。 */
    val wsRetryCount: Int = 0,
    /** WS 最近错误信息。 */
    val wsErrorMessage: String? = null,
    /** 视频区域是否可用（BLE 默认不可用）。 */
    val streamEnabled: Boolean = true,
    /** 当前录制状态。 */
    val recording: Boolean = false,
    /** 已录制时长（秒）。 */
    val recordSeconds: Long = 0L,
    /** 设备心跳时间戳。 */
    val lastHeartbeatAt: Long? = null,
    /** 最近指令链路追踪 ID。 */
    val lastCommandTraceId: String = "",
    /** App-RTMP 测试模式（推流/拉流）。 */
    val testMode: StreamTestMode = StreamTestMode.PULL,
    /** 测试 RTMP 地址输入。 */
    val testRtmpUrl: String = "",
    /** 测试流是否运行中。 */
    val testStreaming: Boolean = false,
    /** Agent 控制台日志（展示用）。 */
    val agentLogs: List<String> = emptyList()
)

/**
 * Control 页面数据状态：
 * - 不直接渲染到 UI，但用于请求拼装与业务上下文。
 */
data class ControlDataState(
    /** 当前登录用户 ID。 */
    val userId: String = ""
)

sealed class ControlIntent {
    data object Initialize : ControlIntent()
    data class UpdateDeviceId(val deviceId: String) : ControlIntent()
    data class SwitchPlatform(val platform: ControlPlatform) : ControlIntent()
    data class SwitchStreamSource(val source: StreamSource) : ControlIntent()
    data class LeftJoystickDrag(val x: Float, val y: Float) : ControlIntent()
    data class RightJoystickDrag(val x: Float, val y: Float) : ControlIntent()
    data object ToggleRecording : ControlIntent()
    data object FetchControlStatus : ControlIntent()
    data object ReconnectControlWs : ControlIntent()
    data object StartTestStream : ControlIntent()
    data object StopTestStream : ControlIntent()
    data class SwitchTestMode(val mode: StreamTestMode) : ControlIntent()
    data class UpdateTestRtmpUrl(val url: String) : ControlIntent()
    data object SendQuickCommandForward : ControlIntent()
    data object SendQuickCommandStop : ControlIntent()
    data object FetchControlAgentLogs : ControlIntent()
}

sealed class ControlEffect {
    data class ShowToast(val message: String) : ControlEffect()
}

enum class ControlPlatform(val label: String, val transportCode: String) {
    CLOUD("云操控平台", "CLOUD"),
    OFFLINE_WIFI("离线WiFi", "OFFLINE_WIFI"),
    OFFLINE_BLE("离线BLE", "OFFLINE_BLE")
}

enum class StreamSource(val label: String) {
    RTMP_DIRECT("RTMP + Nginx"),
    UDP_VIA_SPRING("UDP裸帧经Spring转发")
}

enum class StreamTestMode(val label: String) {
    PUSH("推流"),
    PULL("拉流")
}
