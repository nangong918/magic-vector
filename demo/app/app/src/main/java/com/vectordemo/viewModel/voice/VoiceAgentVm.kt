package com.vectordemo.viewModel.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vectordemo.service.ai.ChatService
import com.vectordemo.service.voice.AliSttEvent
import com.vectordemo.service.voice.AliSttEventType
import com.vectordemo.service.voice.AliSttService
import com.vectordemo.service.voice.OfflineIvwEvent
import com.vectordemo.service.voice.OfflineIvwEventType
import com.vectordemo.service.voice.OfflineIvwService
import com.vectordemo.service.voice.VadEvent
import com.vectordemo.service.voice.VadEventType
import com.vectordemo.service.voice.VadService
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VoiceAgentPhase {
    INITIALIZING, READY, WAKE_DETECTED_WAITING_SPEECH, USER_SPEAKING, USER_SPEECH_ENDED, AGENT_REPLYING, ERROR
}

enum class WakeServiceStatus { DISABLED, ENABLED_IDLE, KEYWORD_DETECTED }
enum class VadServiceStatus { DISABLED, NOISE, SPEECH, TIMEOUT }
enum class SttSendServiceStatus { DISABLED, SENDING, STOPPED_AFTER_VAD_END }
enum class SttReceiveServiceStatus { NO_RESULT, RECEIVING_PARTIAL, FINAL_RECEIVED }
enum class AgentReplyServiceStatus { DISABLED, REPLYING, FINISHED }
enum class VoiceAgentLlmProvider { ALI, XFYUN }

data class VoiceAgentUiState(
    val phase: VoiceAgentPhase = VoiceAgentPhase.INITIALIZING,
    val wakeStatus: WakeServiceStatus = WakeServiceStatus.DISABLED,
    val vadStatus: VadServiceStatus = VadServiceStatus.DISABLED,
    val sttSendStatus: SttSendServiceStatus = SttSendServiceStatus.DISABLED,
    val sttReceiveStatus: SttReceiveServiceStatus = SttReceiveServiceStatus.NO_RESULT,
    val agentReplyStatus: AgentReplyServiceStatus = AgentReplyServiceStatus.DISABLED,
    val llmProvider: VoiceAgentLlmProvider = VoiceAgentLlmProvider.ALI,
    val logs: List<String> = emptyList()
)

class VoiceAgentVm(
    private val ivwService: OfflineIvwService,
    private val vadService: VadService,
    private val sttService: AliSttService,
    private val aliChatService: ChatService,
    private val xfyunChatService: ChatService
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceAgentUiState())
    val uiState: StateFlow<VoiceAgentUiState> = _uiState.asStateFlow()
    private val _effect = Channel<VoiceAgentEffect>(Channel.BUFFERED)
    val effect: Flow<VoiceAgentEffect> = _effect.receiveAsFlow()

    private var systemPrompt: String = ""
    private var recordPermissionGranted = false
    private var ivwAuthPassed = false
    private var wakeListening = false
    private var vadRunning = false
    private var vadSpeechStarted = false
    private var speechEndHandled = false
    private var sttRunning = false
    private var sttStopRequested = false
    private var sttFinalReceived = false
    private var agentCallTriggered = false
    private var latestPartialStt = ""
    private var finalSttText = ""

    private var ivwJob: Job? = null
    private var vadJob: Job? = null
    private var vadDelayedStartJob: Job? = null
    private var vadSpeechTimeoutJob: Job? = null
    private var sttFinalTimeoutJob: Job? = null

    fun initialize(prompt: String) {
        if (_uiState.value.phase != VoiceAgentPhase.INITIALIZING) return
        systemPrompt = prompt
        attachListeners()
        viewModelScope.launch {
            appendLog("初始化中...")
            runCatching {
                appendLog("系统提示词加载完成")
                ivwService.init()
                appendLog("本地能力初始化完成")
            }.onFailure {
                enterError("初始化异常: ${it.message}")
            }
        }
    }

    fun updateRecordPermissionGranted(granted: Boolean) {
        recordPermissionGranted = granted
    }

    fun setLlmProvider(provider: VoiceAgentLlmProvider) {
        if (_uiState.value.llmProvider == provider) return
        _uiState.update { it.copy(llmProvider = provider) }
        appendLog("LLM切换为: ${if (provider == VoiceAgentLlmProvider.ALI) "阿里百炼" else "科大讯飞"}")
    }

    fun buildServiceStatusText(state: VoiceAgentUiState = _uiState.value): String {
        return "唤醒功能：${wakeText(state.wakeStatus)}\n" +
            "VAD功能：${vadText(state.vadStatus)}\n" +
            "STT发送：${sttSendText(state.sttSendStatus)}\n" +
            "STT接收：${sttReceiveText(state.sttReceiveStatus)}\n" +
            "Agent回复：${agentText(state.agentReplyStatus)}"
    }

    fun close() {
        vadDelayedStartJob?.cancel()
        vadSpeechTimeoutJob?.cancel()
        sttFinalTimeoutJob?.cancel()
        ivwJob?.cancel()
        vadJob?.cancel()
        sttService.setListener(null)
        viewModelScope.launch { runCatching { sttService.stop() } }
        ivwService.release()
        vadService.release()
        sttService.release()
    }

    private fun attachListeners() {
        sttService.setListener(::handleSttEvent)

        ivwJob?.cancel()
        ivwJob = viewModelScope.launch {
            ivwService.events().collect { handleIvwEvent(it) }
        }
        vadJob?.cancel()
        vadJob = viewModelScope.launch {
            vadService.events().collect { handleVadEvent(it) }
        }
    }

    private fun handleIvwEvent(event: OfflineIvwEvent) {
        when (event.type) {
            OfflineIvwEventType.AUTH -> {
                ivwAuthPassed = (event.raw["code"] as? Number)?.toInt() == 0
                if (ivwAuthPassed) {
                    appendLog("离线唤醒认证成功")
                    tryEnterReady()
                } else {
                    enterError("离线唤醒认证失败: ${event.message}")
                }
            }
            OfflineIvwEventType.WAKEUP -> {
                if (_uiState.value.phase == VoiceAgentPhase.READY) {
                    startWakeupSession()
                } else {
                    appendLog("收到唤醒事件，但当前阶段不允许处理: ${_uiState.value.phase}")
                }
            }
            OfflineIvwEventType.ERROR -> enterError("离线唤醒异常: ${event.message}")
            OfflineIvwEventType.DB -> if (_uiState.value.phase != VoiceAgentPhase.READY) appendLog(event.message)
            OfflineIvwEventType.LOG, OfflineIvwEventType.STATE -> appendLog(event.message)
        }
    }

    private fun handleVadEvent(event: VadEvent) {
        if (event.type == VadEventType.ERROR) {
            enterError("VAD异常: ${event.message}")
            return
        }
        if (event.type == VadEventType.STATE && event.running != null) {
            vadRunning = event.running
        }
        val phase = _uiState.value.phase
        if (phase != VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH && phase != VoiceAgentPhase.USER_SPEAKING) {
            return
        }
        appendLog("VAD事件: type=${event.type.name}, state=${event.raw["state"] ?: "-"}, msg=${event.message}")

        if (isSpeechEnd(event)) {
            appendLog("VAD检测到结束事件，准备收尾")
            onSpeechEndDetected()
            return
        }
        if (!vadSpeechStarted && isSpeechStart(event)) {
            vadSpeechStarted = true
            vadSpeechTimeoutJob?.cancel()
            _uiState.update { it.copy(phase = VoiceAgentPhase.USER_SPEAKING, vadStatus = VadServiceStatus.SPEECH) }
            appendLog("VAD检测到用户开始说话")
        } else if (!vadSpeechStarted) {
            _uiState.update { it.copy(vadStatus = VadServiceStatus.NOISE) }
        }
    }

    private fun handleSttEvent(event: AliSttEvent) {
        when (event.type) {
            AliSttEventType.STARTED -> {
                sttRunning = true
                _uiState.update { it.copy(sttSendStatus = SttSendServiceStatus.SENDING) }
                appendLog("远端STT启动")
            }
            AliSttEventType.PARTIAL -> {
                latestPartialStt = event.text.orEmpty()
                _uiState.update { it.copy(sttReceiveStatus = SttReceiveServiceStatus.RECEIVING_PARTIAL) }
                appendLog("远端STT结果: ${event.text.orEmpty()}")
            }
            AliSttEventType.FINAL_RESULT -> {
                finalSttText = event.text.orEmpty()
                sttFinalReceived = true
                _uiState.update { it.copy(sttReceiveStatus = SttReceiveServiceStatus.FINAL_RECEIVED) }
                appendLog("远端STT最终结果: ${event.text.orEmpty()}")
                val phase = _uiState.value.phase
                if (phase == VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH || phase == VoiceAgentPhase.USER_SPEAKING) {
                    appendLog("收到最终结果，推进到说话结束态")
                    onSpeechEndDetected()
                } else if (phase == VoiceAgentPhase.USER_SPEECH_ENDED && sttStopRequested) {
                    tryCallAgentAfterSttCompleted(force = false)
                }
            }
            AliSttEventType.STOPPED -> {
                sttRunning = false
                _uiState.update { it.copy(sttSendStatus = SttSendServiceStatus.STOPPED_AFTER_VAD_END) }
                appendLog("远端STT停止")
            }
            AliSttEventType.ERROR -> {
                sttRunning = false
                enterError("远端STT异常: ${event.error}")
            }
        }
    }

    private fun tryEnterReady() {
        if (_uiState.value.phase == VoiceAgentPhase.ERROR) return
        if (!ivwAuthPassed) return
        if (!ensureRecordPermissionBeforeReady()) return
        _uiState.update {
            it.copy(
                phase = VoiceAgentPhase.READY,
                wakeStatus = WakeServiceStatus.ENABLED_IDLE,
                vadStatus = VadServiceStatus.DISABLED,
                sttSendStatus = SttSendServiceStatus.DISABLED,
                sttReceiveStatus = SttReceiveServiceStatus.NO_RESULT,
                agentReplyStatus = AgentReplyServiceStatus.DISABLED
            )
        }
        appendLog("就绪")
        startWakeListeningIfNeeded()
    }

    private fun ensureRecordPermissionBeforeReady(): Boolean {
        val granted = recordPermissionGranted && ivwService.hasRecordPermission()
        if (granted) {
            return true
        }
        appendLog("录音权限未授予，结束页面")
        sendEffect(VoiceAgentEffect.FinishActivity)
        return false
    }

    private fun startWakeListeningIfNeeded() {
        if (_uiState.value.phase != VoiceAgentPhase.READY || wakeListening) return
        viewModelScope.launch {
            runCatching {
                ivwService.startRecordWake("小卡小卡")
                wakeListening = true
                _uiState.update { it.copy(wakeStatus = WakeServiceStatus.ENABLED_IDLE) }
                appendLog("开始唤醒词监听: 小卡小卡")
            }.onFailure {
                enterError("启动唤醒词监听失败: ${it.message}")
            }
        }
    }

    private fun stopWakeListeningIfNeeded() {
        if (!wakeListening) return
        runCatching { ivwService.stopRecordWake() }
        wakeListening = false
        _uiState.update { it.copy(wakeStatus = WakeServiceStatus.DISABLED) }
    }

    private fun startWakeupSession() {
        if (_uiState.value.phase != VoiceAgentPhase.READY) return
        appendLog("唤醒")
        resetRoundFlags()
        _uiState.update {
            it.copy(
                wakeStatus = WakeServiceStatus.KEYWORD_DETECTED,
                vadStatus = VadServiceStatus.DISABLED,
                sttSendStatus = SttSendServiceStatus.SENDING,
                sttReceiveStatus = SttReceiveServiceStatus.NO_RESULT,
                agentReplyStatus = AgentReplyServiceStatus.DISABLED
            )
        }
        viewModelScope.launch {
            runCatching {
                stopWakeListeningIfNeeded()
                sttService.start()
                sttRunning = true
                _uiState.update { it.copy(phase = VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH) }
                appendLog("STT已启动，0~2秒不启用VAD检测")
                scheduleVadStartAfterDelay()
            }.onFailure {
                enterError("唤醒后流程启动失败: ${it.message}")
            }
        }
    }

    private fun scheduleVadStartAfterDelay() {
        vadDelayedStartJob?.cancel()
        vadSpeechTimeoutJob?.cancel()
        vadDelayedStartJob = viewModelScope.launch {
            delay(2000)
            if (_uiState.value.phase != VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH || speechEndHandled) return@launch
            runCatching {
                startSileroVad()
                appendLog("VAD已启动，开始检测说话状态（2秒窗口）")
                _uiState.update { it.copy(vadStatus = VadServiceStatus.NOISE) }
                vadSpeechTimeoutJob = launch {
                    delay(2000)
                    if (_uiState.value.phase == VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH && !vadSpeechStarted && !speechEndHandled) {
                        _uiState.update { it.copy(vadStatus = VadServiceStatus.TIMEOUT) }
                        appendLog("VAD超时：未检测到开始说话，按0~2秒已说完处理")
                        onSpeechEndDetected()
                    }
                }
            }.onFailure {
                enterError("延迟启动VAD失败: ${it.message}")
            }
        }
    }

    private fun startSileroVad() {
        val opts = vadService.getOptions()
        val sampleRate = pick(opts.sampleRates, "SAMPLE_RATE_8K") ?: error("Silero参数不可用")
        val mode = pick(opts.modes, "NORMAL") ?: error("Silero参数不可用")
        val frameOpts = vadService.getOptions(sampleRate)
        val frameSize = pick(frameOpts.frameSizes, "FRAME_SIZE_256") ?: error("Silero参数不可用")

        vadService.stop()
        vadService.start(sampleRate, frameSize, mode)
        vadRunning = true
        _uiState.update { it.copy(vadStatus = VadServiceStatus.NOISE) }
        appendLog("Silero参数: sampleRate=$sampleRate, frameSize=$frameSize, mode=$mode")
    }

    private fun stopSileroVad(logWhenStopped: String? = null) {
        vadService.stop()
        vadRunning = false
        _uiState.update { it.copy(vadStatus = VadServiceStatus.DISABLED) }
        if (!logWhenStopped.isNullOrBlank()) appendLog(logWhenStopped)
    }

    private fun onSpeechEndDetected() {
        if (speechEndHandled) return
        val phase = _uiState.value.phase
        if (phase != VoiceAgentPhase.WAKE_DETECTED_WAITING_SPEECH && phase != VoiceAgentPhase.USER_SPEAKING) return
        speechEndHandled = true
        vadDelayedStartJob?.cancel()
        vadSpeechTimeoutJob?.cancel()
        _uiState.update { it.copy(phase = VoiceAgentPhase.USER_SPEECH_ENDED) }
        appendLog("检测到用户说话结束")

        viewModelScope.launch {
            runCatching {
                if (vadRunning) stopSileroVad("VAD已关闭（说话结束）")
                sttStopRequested = true
                _uiState.update { it.copy(sttSendStatus = SttSendServiceStatus.STOPPED_AFTER_VAD_END) }
                if (sttRunning) {
                    sttService.stop()
                    appendLog("已停止向远端STT传输音频")
                }
                sttFinalTimeoutJob?.cancel()
                sttFinalTimeoutJob = launch {
                    delay(6000)
                    if (_uiState.value.phase == VoiceAgentPhase.USER_SPEECH_ENDED) {
                        appendLog("等待STT最终结果超时，使用当前结果继续")
                        tryCallAgentAfterSttCompleted(force = true)
                    }
                }
            }.onFailure {
                enterError("结束说话流程失败: ${it.message}")
            }
        }
    }

    private fun tryCallAgentAfterSttCompleted(force: Boolean) {
        if (agentCallTriggered || _uiState.value.phase != VoiceAgentPhase.USER_SPEECH_ENDED) return
        if (!force && !sttFinalReceived) return
        val text = if (finalSttText.trim().isNotEmpty()) finalSttText.trim() else latestPartialStt.trim()
        if (text.isBlank()) {
            appendLog("异常: STT未返回有效结果")
            recoverAfterSttNoResult()
            return
        }
        agentCallTriggered = true
        callAgentWithText(text)
    }

    private fun callAgentWithText(userText: String) {
        if (_uiState.value.phase == VoiceAgentPhase.ERROR) return
        sttFinalTimeoutJob?.cancel()
        _uiState.update {
            it.copy(
                phase = VoiceAgentPhase.AGENT_REPLYING,
                wakeStatus = WakeServiceStatus.DISABLED,
                vadStatus = VadServiceStatus.DISABLED,
                sttSendStatus = SttSendServiceStatus.DISABLED,
                agentReplyStatus = AgentReplyServiceStatus.REPLYING
            )
        }
        appendLog("开始调用Agent: $userText")

        viewModelScope.launch {
            runCatching {
                selectedChatService().sendChat(
                    systemPrompt = systemPrompt,
                    history = emptyList(),
                    userMessage = userText,
                    onDelta = { if (it.isNotBlank()) appendLog("AI回复流: $it") },
                    onDone = { appendLog("AI回复完毕") }
                )
            }.onFailure {
                enterError("Agent调用失败: ${it.message}")
                return@launch
            }
            if (_uiState.value.phase == VoiceAgentPhase.ERROR) return@launch
            _uiState.update { it.copy(agentReplyStatus = AgentReplyServiceStatus.FINISHED) }
            resetRoundFlags()
            tryEnterReady()
        }
    }

    private fun selectedChatService(): ChatService {
        return if (_uiState.value.llmProvider == VoiceAgentLlmProvider.ALI) aliChatService else xfyunChatService
    }

    private fun recoverAfterSttNoResult() {
        _uiState.update {
            it.copy(
                sttSendStatus = SttSendServiceStatus.DISABLED,
                sttReceiveStatus = SttReceiveServiceStatus.NO_RESULT,
                vadStatus = VadServiceStatus.DISABLED,
                agentReplyStatus = AgentReplyServiceStatus.DISABLED
            )
        }
        resetRoundFlags()
        tryEnterReady()
    }

    private fun enterError(message: String) {
        appendLog("异常: $message")
        _uiState.update {
            it.copy(
                phase = VoiceAgentPhase.ERROR,
                wakeStatus = WakeServiceStatus.DISABLED,
                vadStatus = VadServiceStatus.DISABLED,
                sttSendStatus = SttSendServiceStatus.DISABLED,
                agentReplyStatus = AgentReplyServiceStatus.DISABLED
            )
        }
        stopAllFeatures()
        recoverWakeAfterError()
    }

    private fun stopAllFeatures() {
        vadDelayedStartJob?.cancel()
        vadSpeechTimeoutJob?.cancel()
        sttFinalTimeoutJob?.cancel()
        stopWakeListeningIfNeeded()
        runCatching { stopSileroVad() }
        viewModelScope.launch { runCatching { sttService.stop() } }
        sttRunning = false
    }

    private fun recoverWakeAfterError() {
        viewModelScope.launch {
            delay(500)
            if (ivwAuthPassed) {
                appendLog("异常后自动恢复唤醒监听")
                tryEnterReady()
            }
        }
    }

    private fun resetRoundFlags() {
        vadDelayedStartJob?.cancel()
        vadSpeechTimeoutJob?.cancel()
        latestPartialStt = ""
        finalSttText = ""
        vadSpeechStarted = false
        speechEndHandled = false
        sttStopRequested = false
        sttFinalReceived = false
        agentCallTriggered = false
        sttRunning = false
    }

    private fun appendLog(message: String) {
        if (message.isBlank()) return
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val next = _uiState.value.logs.toMutableList()
        next.add("[$time] $message")
        if (next.size > 300) {
            repeat(next.size - 300) { next.removeAt(0) }
        }
        _uiState.update { it.copy(logs = next) }
    }

    private fun isSpeechEnd(event: VadEvent): Boolean {
        val stateText = event.raw["state"]?.toString()?.lowercase().orEmpty()
        val msg = event.message.lowercase()
        val raw = event.raw.toString().lowercase()
        return stateText.contains("end") ||
            stateText.contains("stop") ||
            stateText.contains("silence") ||
            stateText.contains("idle") ||
            msg.contains("说话结束") ||
            msg.contains("停止说话") ||
            msg.contains("speech end") ||
            msg.contains("stop_speech") ||
            raw.contains("speech=false") ||
            raw.contains("speaking=false") ||
            raw.contains("voice=false") ||
            event.raw["type"]?.toString() == "stop_speech"
    }

    private fun isSpeechStart(event: VadEvent): Boolean {
        val stateText = event.raw["state"]?.toString()?.lowercase().orEmpty()
        val msg = event.message.lowercase()
        val raw = event.raw.toString().lowercase()
        return stateText.contains("start") ||
            stateText.contains("speech_start") ||
            msg.contains("开始说话") ||
            msg.contains("检测到开始说话") ||
            msg.contains("start_speech") ||
            raw.contains("speech=true") ||
            raw.contains("speaking=true") ||
            raw.contains("voice=true") ||
            event.raw["type"]?.toString() == "start_speech"
    }

    private fun wakeText(status: WakeServiceStatus): String = when (status) {
        WakeServiceStatus.DISABLED -> "禁用"
        WakeServiceStatus.ENABLED_IDLE -> "启用未被唤醒"
        WakeServiceStatus.KEYWORD_DETECTED -> "启用检测到关键词"
    }

    private fun vadText(status: VadServiceStatus): String = when (status) {
        VadServiceStatus.DISABLED -> "禁用"
        VadServiceStatus.NOISE -> "检测到噪音"
        VadServiceStatus.SPEECH -> "检测到说话"
        VadServiceStatus.TIMEOUT -> "VAD超时"
    }

    private fun sttSendText(status: SttSendServiceStatus): String = when (status) {
        SttSendServiceStatus.DISABLED -> "禁用"
        SttSendServiceStatus.SENDING -> "正在发送"
        SttSendServiceStatus.STOPPED_AFTER_VAD_END -> "VAD结束并结束STT发送"
    }

    private fun sttReceiveText(status: SttReceiveServiceStatus): String = when (status) {
        SttReceiveServiceStatus.NO_RESULT -> "无结果"
        SttReceiveServiceStatus.RECEIVING_PARTIAL -> "正在接收中间结果"
        SttReceiveServiceStatus.FINAL_RECEIVED -> "已经接收最终结果"
    }

    private fun agentText(status: AgentReplyServiceStatus): String = when (status) {
        AgentReplyServiceStatus.DISABLED -> "禁用"
        AgentReplyServiceStatus.REPLYING -> "正在回复"
        AgentReplyServiceStatus.FINISHED -> "回复完成"
    }

    private fun pick(values: List<String>, preferred: String): String? {
        if (values.isEmpty()) return null
        return if (values.contains(preferred)) preferred else values.first()
    }

    companion object {
        fun factory(
            ivwService: OfflineIvwService,
            vadService: VadService,
            sttService: AliSttService,
            aliChatService: ChatService,
            xfyunChatService: ChatService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VoiceAgentVm(
                    ivwService = ivwService,
                    vadService = vadService,
                    sttService = sttService,
                    aliChatService = aliChatService,
                    xfyunChatService = xfyunChatService
                ) as T
            }
        }
    }

    private fun sendEffect(effect: VoiceAgentEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

sealed class VoiceAgentEffect {
    data object FinishActivity : VoiceAgentEffect()
}

