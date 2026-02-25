package com.demo.flutternew.manager

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.demo.aarlib.vad.common.VadEventListener
import com.demo.aarlib.vad.silero.SileroVadBridge
import com.demo.aarlib.vad.webrtc.WebRtcVadBridge
import com.demo.aarlib.vad.yamnet.YamnetVadBridge
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class VadBridgeManager(private val activity: FlutterActivity) {
    private val vadChannel = "com.demo.flutternew/vad"
    private val vadEventChannel = "com.demo.flutternew/vad_event"
    private val reqRecordAudio = 0x68

    private var eventSink: EventChannel.EventSink? = null
    private var pendingRecordPermissionResult: MethodChannel.Result? = null
    private var runningEngine: String? = null

    private val webRtcListener = VadEventListener { payload -> emitEvent("webrtc", payload) }
    private val sileroListener = VadEventListener { payload -> emitEvent("silero", payload) }
    private val yamnetListener = VadEventListener { payload -> emitEvent("yamnet", payload) }

    fun registerWith(flutterEngine: FlutterEngine) {
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, vadEventChannel)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                    bindEventListeners()
                }

                override fun onCancel(arguments: Any?) {
                    eventSink = null
                    clearEventListeners()
                }
            })

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, vadChannel).setMethodCallHandler { call, result ->
            when (call.method) {
                "requestRecordPermission" -> {
                    if (hasRecordPermission()) {
                        result.success(true)
                    } else {
                        pendingRecordPermissionResult = result
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            reqRecordAudio
                        )
                    }
                }

                "getEngines" -> {
                    result.success(listOf("webrtc", "silero", "yamnet"))
                }

                "getOptions" -> {
                    val engine = normalizeEngine(call.argument<String>("engine"))
                    val sampleRateArg = call.argument<String>("sampleRate").orEmpty()
                    val options = buildOptions(engine, sampleRateArg)
                    result.success(options)
                }

                "startVad" -> {
                    if (!hasRecordPermission()) {
                        result.error("NO_PERMISSION", "缺少录音权限: RECORD_AUDIO", null)
                        return@setMethodCallHandler
                    }

                    val engine = normalizeEngine(call.argument<String>("engine"))
                    val sampleRate = call.argument<String>("sampleRate").orEmpty()
                    val frameSize = call.argument<String>("frameSize").orEmpty()
                    val mode = call.argument<String>("mode").orEmpty()
                    startVad(engine, sampleRate, frameSize, mode)
                    result.success(null)
                }

                "stopVad" -> {
                    val engine = normalizeEngine(call.argument<String>("engine"))
                    stopVad(engine)
                    result.success(null)
                }

                "releaseVad" -> {
                    releaseAll()
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        }
    }

    fun onStart() {
        if (eventSink != null) {
            bindEventListeners()
        }
    }

    fun onStop() {
        clearEventListeners()
        stopAll()
    }

    fun onDestroy() {
        pendingRecordPermissionResult?.success(false)
        pendingRecordPermissionResult = null
        clearEventListeners()
        releaseAll()
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != reqRecordAudio) {
            return
        }
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        pendingRecordPermissionResult?.success(granted)
        pendingRecordPermissionResult = null
    }

    private fun hasRecordPermission(): Boolean {
        return WebRtcVadBridge.hasRecordPermission(activity)
    }

    private fun normalizeEngine(raw: String?): String {
        return when (raw?.trim()?.lowercase()) {
            "silero" -> "silero"
            "yamnet" -> "yamnet"
            else -> "webrtc"
        }
    }

    private fun buildOptions(engine: String, sampleRate: String): Map<String, Any> {
        val safeSampleRates: List<String>
        val safeFrameSizes: List<String>
        val safeModes: List<String>
        when (engine) {
            "silero" -> {
                safeSampleRates = SileroVadBridge.getSampleRates() ?: emptyList()
                safeFrameSizes = if (sampleRate.isNotBlank()) {
                    SileroVadBridge.getFrameSizes(sampleRate) ?: emptyList()
                } else {
                    SileroVadBridge.getFrameSizes() ?: emptyList()
                }
                safeModes = SileroVadBridge.getModes() ?: emptyList()
            }

            "yamnet" -> {
                safeSampleRates = YamnetVadBridge.getSampleRates() ?: emptyList()
                safeFrameSizes = YamnetVadBridge.getFrameSizes() ?: emptyList()
                safeModes = YamnetVadBridge.getModes() ?: emptyList()
            }

            else -> {
                safeSampleRates = WebRtcVadBridge.getSampleRates() ?: emptyList()
                safeFrameSizes = if (sampleRate.isNotBlank()) {
                    WebRtcVadBridge.getFrameSizes(sampleRate) ?: emptyList()
                } else {
                    WebRtcVadBridge.getFrameSizes() ?: emptyList()
                }
                safeModes = WebRtcVadBridge.getModes() ?: emptyList()
            }
        }
        return hashMapOf(
            "sampleRates" to safeSampleRates,
            "frameSizes" to safeFrameSizes,
            "modes" to safeModes
        )
    }

    private fun startVad(engine: String, sampleRate: String, frameSize: String, mode: String) {
        if (runningEngine != null && runningEngine != engine) {
            stopVad(runningEngine)
        }
        when (engine) {
            "silero" -> {
                SileroVadBridge.updateConfig(sampleRate, frameSize, mode)
                SileroVadBridge.start(activity)
            }

            "yamnet" -> {
                YamnetVadBridge.updateConfig(sampleRate, frameSize, mode)
                YamnetVadBridge.start(activity)
            }

            else -> {
                WebRtcVadBridge.updateConfig(sampleRate, frameSize, mode)
                WebRtcVadBridge.start(activity)
            }
        }
        runningEngine = engine
    }

    private fun stopVad(engine: String?) {
        when (normalizeEngine(engine)) {
            "silero" -> SileroVadBridge.stop()
            "yamnet" -> YamnetVadBridge.stop()
            else -> WebRtcVadBridge.stop()
        }
        if (runningEngine == normalizeEngine(engine)) {
            runningEngine = null
        }
    }

    private fun stopAll() {
        WebRtcVadBridge.stop()
        SileroVadBridge.stop()
        YamnetVadBridge.stop()
        runningEngine = null
    }

    private fun releaseAll() {
        stopAll()
        WebRtcVadBridge.release()
        SileroVadBridge.release()
        YamnetVadBridge.release()
    }

    private fun bindEventListeners() {
        WebRtcVadBridge.setEventListener(webRtcListener)
        SileroVadBridge.setEventListener(sileroListener)
        YamnetVadBridge.setEventListener(yamnetListener)
    }

    private fun clearEventListeners() {
        WebRtcVadBridge.clearEventListener()
        SileroVadBridge.clearEventListener()
        YamnetVadBridge.clearEventListener()
    }

    private fun emitEvent(engine: String, payload: Map<String, Any>) {
        activity.runOnUiThread {
            val copy = HashMap<String, Any>(payload)
            copy["engine"] = engine
            eventSink?.success(copy)
        }
    }
}
