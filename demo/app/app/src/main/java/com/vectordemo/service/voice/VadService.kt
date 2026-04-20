package com.vectordemo.service.voice

import android.content.Context
import com.demo.aarlib.vad.silero.SileroVadBridge
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class VadOptions(
    val sampleRates: List<String>,
    val frameSizes: List<String>,
    val modes: List<String>
)

enum class VadEventType {
    LOG, DB, STATE, ERROR, OTHER
}

data class VadEvent(
    val type: VadEventType,
    val message: String,
    val running: Boolean?,
    val db: Int?,
    val raw: Map<String, Any?>
)

class VadService(private val context: Context) {
    fun hasRecordPermission(): Boolean = SileroVadBridge.hasRecordPermission(context)

    fun getOptions(sampleRate: String? = null): VadOptions {
        val sampleRates = SileroVadBridge.getSampleRates()
        val frameSizes = if (sampleRate.isNullOrBlank()) {
            SileroVadBridge.getFrameSizes()
        } else {
            SileroVadBridge.getFrameSizes(sampleRate)
        }
        val modes = SileroVadBridge.getModes()
        return VadOptions(sampleRates, frameSizes, modes)
    }

    fun start(sampleRate: String, frameSize: String, mode: String) {
        SileroVadBridge.updateConfig(sampleRate, frameSize, mode)
        SileroVadBridge.start(context)
    }

    fun stop() {
        SileroVadBridge.stop()
    }

    fun release() {
        SileroVadBridge.release()
    }

    fun events(): Flow<VadEvent> = callbackFlow {
        SileroVadBridge.setEventListener { payload ->
            val map = payload.mapValues { it.value }
            val type = parseType(map["type"]?.toString().orEmpty())
            val db = (map["db"] as? Number)?.toInt()
            val running = map["running"] as? Boolean
            trySend(
                VadEvent(
                    type = type,
                    message = map["message"]?.toString().orEmpty(),
                    running = running,
                    db = db,
                    raw = map
                )
            )
        }
        awaitClose { SileroVadBridge.clearEventListener() }
    }

    private fun parseType(type: String): VadEventType {
        return when (type) {
            "db" -> VadEventType.DB
            "state" -> VadEventType.STATE
            "error" -> VadEventType.ERROR
            "start_speech", "speeching", "stop_speech", "log" -> VadEventType.LOG
            else -> VadEventType.OTHER
        }
    }
}

