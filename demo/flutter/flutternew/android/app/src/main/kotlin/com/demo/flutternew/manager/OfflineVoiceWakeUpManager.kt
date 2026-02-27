package com.demo.flutternew.manager

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.demo.aarlib.voicewakeup.VoiceWakeUpBridge
import com.demo.aarlib.voicewakeup.VoiceWakeUpEventListener
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import java.io.File

class OfflineVoiceWakeUpManager(private val activity: FlutterActivity) {
    private val ivwChannel = "com.demo.flutternew/ivw"
    private val ivwEventChannel = "com.demo.flutternew/ivw_event"
    private val reqRecordAudio = 0x66

    private var eventSink: EventChannel.EventSink? = null
    private var pendingRecordPermissionResult: MethodChannel.Result? = null
    private var configReady = false
    private var sdkConfig: IvwSdkConfig? = null

    private val eventListener = VoiceWakeUpEventListener { payload ->
        emitEvent(payload)
    }

    fun registerWith(flutterEngine: FlutterEngine) {
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, ivwEventChannel)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                    VoiceWakeUpBridge.setEventListener(eventListener)
                }

                override fun onCancel(arguments: Any?) {
                    eventSink = null
                    VoiceWakeUpBridge.clearEventListener()
                }
            })

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, ivwChannel).setMethodCallHandler {
            call,
            result ->
            when (call.method) {
                "initSdk" -> {
                    val configMap = call.argument<Map<String, Any?>>("config")
                    val parsed = parseConfig(configMap)
                    if (parsed == null) {
                        result.error("INVALID_CONFIG", "initSdk缺少有效配置: appId/apiKey/apiSecret", null)
                        return@setMethodCallHandler
                    }
                    sdkConfig = parsed
                    configReady = false
                    ensureBridgeConfig()
                    VoiceWakeUpBridge.initSdk(activity)
                    result.success(null)
                }

                "startRecordWake" -> {
                    val keyword = call.argument<String>("keyword") ?: "你好小迪"
                    if (!VoiceWakeUpBridge.hasRecordPermission(activity)) {
                        result.error("NO_PERMISSION", "缺少录音权限: RECORD_AUDIO", null)
                        return@setMethodCallHandler
                    }
                    VoiceWakeUpBridge.startRecordWake(activity, keyword)
                    result.success(null)
                }

                "stopRecordWake" -> {
                    VoiceWakeUpBridge.stopRecordWake()
                    result.success(null)
                }

                "requestRecordPermission" -> {
                    if (VoiceWakeUpBridge.hasRecordPermission(activity)) {
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

                "startFileWake" -> {
                    val keyword = call.argument<String>("keyword") ?: "你好小迪"
                    val path = call.argument<String>("audioPath").orEmpty()
                    if (path.isBlank()) {
                        result.error("INVALID_ARGS", "audioPath不能为空", null)
                        return@setMethodCallHandler
                    }
                    VoiceWakeUpBridge.startFileWake(activity, keyword, path)
                    result.success(null)
                }

                "releaseIvw" -> {
                    VoiceWakeUpBridge.release()
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        }
    }

    fun onStart() {
        if (eventSink != null) {
            VoiceWakeUpBridge.setEventListener(eventListener)
        }
    }

    fun onStop() {
        VoiceWakeUpBridge.clearEventListener()
    }

    fun onDestroy() {
        pendingRecordPermissionResult?.success(false)
        pendingRecordPermissionResult = null
        VoiceWakeUpBridge.clearEventListener()
        VoiceWakeUpBridge.release()
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

    private fun ensureBridgeConfig() {
        if (configReady) {
            return
        }
        val cfg = sdkConfig
        if (cfg == null) {
            emitEvent(
                mapOf(
                    "type" to "error",
                    "message" to "未提供离线唤醒SDK配置，请先通过initSdk传入config"
                )
            )
            return
        }
        val workDir = resolveWritableWorkDir()
        VoiceWakeUpBridge.setDefaultConfig(
            VoiceWakeUpBridge.Config(
                cfg.appId,
                cfg.apiKey,
                cfg.apiSecret,
                workDir,
                cfg.abilityId
            )
        )
        configReady = true
    }

    private fun parseConfig(config: Map<String, Any?>?): IvwSdkConfig? {
        if (config == null) {
            return null
        }
        val appId = config["appId"]?.toString()?.trim().orEmpty()
        val apiKey = config["apiKey"]?.toString()?.trim().orEmpty()
        val apiSecret = config["apiSecret"]?.toString()?.trim().orEmpty()
        val abilityId = config["abilityId"]?.toString()?.trim().orEmpty()
        if (appId.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            return null
        }
        return IvwSdkConfig(
            appId = appId,
            apiKey = apiKey,
            apiSecret = apiSecret,
            abilityId = if (abilityId.isBlank()) DEFAULT_ABILITY_ID else abilityId
        )
    }

    private fun resolveWritableWorkDir(): String {
        val baseDir = activity.getExternalFilesDir(null) ?: activity.filesDir
        val iflytekDir = File(baseDir, "iflytek")
        val absolute = iflytekDir.absolutePath
        return if (absolute.endsWith(File.separator)) absolute else "$absolute${File.separator}"
    }

    private fun emitEvent(payload: Map<String, Any?>) {
        activity.runOnUiThread {
            eventSink?.success(HashMap(payload))
        }
    }

    private data class IvwSdkConfig(
        val appId: String,
        val apiKey: String,
        val apiSecret: String,
        val abilityId: String
    )

    private companion object {
        private const val DEFAULT_ABILITY_ID = "e867a88f2"
    }
}
