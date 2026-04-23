package com.vectordemo.service.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.vectordemo.config.AliSttKeyConfig
import com.vectordemo.config.ModuleKeyConfigStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AliSttEventType {
    STARTED, PARTIAL, FINAL_RESULT, STOPPED, ERROR
}

data class AliSttEvent(
    val type: AliSttEventType,
    val text: String? = null,
    val error: String? = null
)

class AliSttService(private val context: Context) {
    private companion object {
        const val TAG = "AliSttService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder().build()

    private var listener: ((AliSttEvent) -> Unit)? = null
    private var webSocket: WebSocket? = null
    private var wsJob: CompletableDeferred<Unit>? = null
    private var taskId: String? = null
    private var taskStarted = false
    private var finalEmitted = false
    private var running = false
    private var sessionActive = false
    private var taskFailed = false
    private var taskFailedError: String? = null
    private var socketClosed = false
    private var manualClosing = false
    private var audioJob: Job? = null
    private var record: AudioRecord? = null
    private val sentenceResults = mutableListOf<String>()
    private var latestCombinedText: String = ""
    private var audioFramesSent = 0
    private var audioBytesSent = 0
    private var runTaskAtMs = 0L

    fun setListener(listener: ((AliSttEvent) -> Unit)?) {
        this.listener = listener
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun start() = withContext(Dispatchers.IO) {
        if (running) return@withContext
        if (!hasRecordPermission()) {
            emit(AliSttEvent(AliSttEventType.ERROR, error = "麦克风权限未授权"))
            return@withContext
        }

        val config = ModuleKeyConfigStore.load(context).aliStt
        resetSessionState()
        running = true
        sessionActive = true
        Log.d(TAG, "start: begin new session")
        emit(AliSttEvent(AliSttEventType.STARTED))

        try {
            openAndRunTask(config)
            waitTaskStarted()
            if (!running || !sessionActive || taskFailed || socketClosed || webSocket == null) {
                throw IllegalStateException(taskFailedError ?: "STT会话已中断")
            }
            startRecordingAndSend(config)
            Log.d(TAG, "start: task started, recording loop running")
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            emit(AliSttEvent(AliSttEventType.ERROR, error = e.message ?: "STT启动失败"))
            cleanupOnError()
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        if (!running) return@withContext
        Log.d(TAG, "stop: taskStarted=$taskStarted frames=$audioFramesSent bytes=$audioBytesSent")
        running = false
        sessionActive = false
        emit(AliSttEvent(AliSttEventType.STOPPED))
        stopRecording()
        if (taskStarted && !socketClosed && webSocket != null) {
            sendFinishTask()
            delay(200)
        } else {
            closeSocket()
        }
    }

    fun release() {
        scope.launch {
            running = false
            sessionActive = false
            stopRecording()
            closeSocket()
            scope.cancel()
        }
    }

    private fun openAndRunTask(config: AliSttKeyConfig) {
        taskId = UUID.randomUUID().toString().replace("-", "")
        wsJob = CompletableDeferred()
        Log.d(TAG, "openAndRunTask: taskId=$taskId url=${config.hostUrl}")
        val request = Request.Builder()
            .url(config.hostUrl)
            .addHeader("Authorization", "bearer ${config.apiKey}")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "websocket opened, send run-task")
                sendRunTask(config)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleSocketMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socketClosed = true
                if (manualClosing) {
                    Log.d(TAG, "websocket closed manually")
                    return
                }
                Log.e(TAG, "websocket failure", t)
                wsJob?.completeExceptionally(t)
                if (running || sessionActive) {
                    emit(AliSttEvent(AliSttEventType.ERROR, error = t.message ?: "WebSocket失败"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socketClosed = true
                if (wsJob?.isCompleted == false) {
                    wsJob?.complete(Unit)
                }
            }
        })
    }

    private suspend fun waitTaskStarted() {
        val job = wsJob ?: throw IllegalStateException("STT任务未创建")
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.withTimeout(6000) {
                while (!taskStarted && isActive) {
                    delay(50)
                }
                if (!taskStarted) {
                    throw IllegalStateException(taskFailedError ?: "阿里STT任务启动超时")
                }
            }
        }
        if (job.isCancelled) {
            throw IllegalStateException("阿里STT任务启动失败")
        }
        Log.d(TAG, "waitTaskStarted: success taskId=$taskId")
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecordingAndSend(config: AliSttKeyConfig) {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(config.sampleRate, channelConfig, audioFormat)
        val bufferSize = if (minBuf <= 0) 3200 else maxOf(minBuf, 3200)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            config.sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        record = audioRecord
        audioRecord.startRecording()

        audioJob = scope.launch {
            val buffer = ByteArray(3200)
            while (isActive && running) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0 && taskStarted && !taskFailed && !socketClosed) {
                    audioFramesSent++
                    audioBytesSent += read
                    webSocket?.send(buffer.copyOf(read).toByteString())
                }
            }
        }
    }

    private fun stopRecording() {
        audioJob?.cancel()
        audioJob = null
        runCatching {
            record?.stop()
        }
        runCatching {
            record?.release()
        }
        record = null
    }

    private fun sendRunTask(config: AliSttKeyConfig) {
        val params = JSONObject()
            .put("format", config.format)
            .put("sample_rate", config.sampleRate)
            .put("disfluency_removal_enabled", config.disfluencyRemovalEnabled)
        if (config.languageHints.isNotEmpty()) {
            params.put("language_hints", JSONArray(config.languageHints))
        }
        val body = JSONObject()
            .put(
                "header",
                JSONObject()
                    .put("action", "run-task")
                    .put("task_id", taskId)
                    .put("streaming", "duplex")
            )
            .put(
                "payload",
                JSONObject()
                    .put("task_group", "audio")
                    .put("task", "asr")
                    .put("function", "recognition")
                    .put("model", config.model)
                    .put("parameters", params)
                    .put("input", JSONObject())
            )
        webSocket?.send(body.toString())
        runTaskAtMs = System.currentTimeMillis()
    }

    private fun sendFinishTask() {
        val body = JSONObject()
            .put(
                "header",
                JSONObject()
                    .put("action", "finish-task")
                    .put("task_id", taskId)
                    .put("streaming", "duplex")
            )
            .put("payload", JSONObject().put("input", JSONObject()))
        webSocket?.send(body.toString())
    }

    private fun handleSocketMessage(message: String) {
        val json = runCatching { JSONObject(message) }.getOrNull() ?: return
        val header = json.optJSONObject("header") ?: JSONObject()
        val payload = json.optJSONObject("payload") ?: JSONObject()
        val event = header.optString("event")
        when (event) {
            "task-started" -> {
                taskStarted = true
                wsJob?.complete(Unit)
                Log.d(TAG, "event task-started: taskId=$taskId, costMs=${System.currentTimeMillis() - runTaskAtMs}")
            }
            "result-generated" -> handleResultGenerated(payload)
            "task-finished" -> {
                Log.d(TAG, "event task-finished: frames=$audioFramesSent bytes=$audioBytesSent")
                emitFinalIfNeeded()
                closeSocket()
            }
            "task-failed" -> {
                val code = header.opt("error_code")
                val msg = header.opt("error_message")
                taskFailed = true
                running = false
                sessionActive = false
                taskFailedError = "task-failed: code=$code msg=$msg"
                if (wsJob?.isCompleted == false) {
                    wsJob?.completeExceptionally(IllegalStateException(taskFailedError))
                }
                Log.e(
                    TAG,
                    "event task-failed: code=$code msg=$msg frames=$audioFramesSent bytes=$audioBytesSent taskStarted=$taskStarted"
                )
                emit(
                    AliSttEvent(
                        AliSttEventType.ERROR,
                        error = "$taskFailedError frames=$audioFramesSent bytes=$audioBytesSent"
                    )
                )
                closeSocket()
            }
            else -> {
                if (event.isNotEmpty()) {
                    Log.d(TAG, "event $event")
                }
            }
        }
    }

    private fun handleResultGenerated(payload: JSONObject) {
        val sentence = payload.optJSONObject("output")
            ?.optJSONObject("sentence")
            ?: return
        if (sentence.optBoolean("heartbeat", false)) {
            return
        }
        val text = sentence.optString("text").orEmpty()
        if (text.isBlank()) {
            return
        }
        val sentenceEnd = sentence.optBoolean("sentence_end", false)
        if (sentenceEnd) {
            sentenceResults.add(text)
            latestCombinedText = sentenceResults.joinToString(" ").trim()
            emit(AliSttEvent(AliSttEventType.PARTIAL, text = latestCombinedText))
            return
        }
        latestCombinedText = if (sentenceResults.isEmpty()) {
            text
        } else {
            "${sentenceResults.joinToString(" ")} $text".trim()
        }
        emit(AliSttEvent(AliSttEventType.PARTIAL, text = latestCombinedText))
    }

    private fun emitFinalIfNeeded() {
        if (finalEmitted) return
        finalEmitted = true
        emit(AliSttEvent(AliSttEventType.FINAL_RESULT, text = latestCombinedText.trim()))
    }

    private fun cleanupOnError() {
        running = false
        sessionActive = false
        stopRecording()
        closeSocket()
    }

    private fun closeSocket() {
        manualClosing = true
        runCatching { webSocket?.close(1000, "close") }
        socketClosed = true
        webSocket = null
    }

    private fun resetSessionState() {
        taskStarted = false
        finalEmitted = false
        taskId = null
        sentenceResults.clear()
        latestCombinedText = ""
        wsJob = null
        taskFailed = false
        taskFailedError = null
        socketClosed = false
        manualClosing = false
        audioFramesSent = 0
        audioBytesSent = 0
        runTaskAtMs = 0L
    }

    private fun emit(event: AliSttEvent) {
        listener?.invoke(event)
    }
}

