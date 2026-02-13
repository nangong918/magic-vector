package com.demo.flutternew

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.net.wifi.WifiInfo
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import com.iflytek.aikit.core.AiAudio
import com.iflytek.aikit.core.AiHandle
import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiListener
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.AiStatus
import com.iflytek.aikit.core.BaseLibrary
import com.iflytek.aikit.core.CoreListener
import com.iflytek.aikit.core.ErrType
import com.iflytek.aikit.core.LogLvl
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity: FlutterActivity() {
    // 1. 电量Channel（原有）
    private val BATTERY_CHANNEL = "com.demo.flutternew/battery"
    // 2. 新增WiFi Channel（需与Flutter端一致）
    private val WIFI_CHANNEL = "com.demo.flutternew/wifi"
    // 3. 离线唤醒 MethodChannel / EventChannel
    private val IVW_CHANNEL = "com.demo.flutternew/ivw"
    private val IVW_EVENT_CHANNEL = "com.demo.flutternew/ivw_event"

    private val ABILITY_ID = "e867a88f2"
    private val BUFFER_SIZE = 1280
    private val WRITE_CHUNK_SIZE = 320

    private lateinit var ivwHandlerThread: HandlerThread
    private lateinit var ivwHandler: Handler
    private var eventSink: EventChannel.EventSink? = null

    private var appId: String = ""
    private var apiKey: String = ""
    private var apiSecret: String = ""
    private var workDir: String = ""
    private var resDir: String = ""

    private var aiHandle: AiHandle? = null
    private var audioRecord: AudioRecord? = null
    private var audioPath: String = ""
    private val isEnd = AtomicBoolean(true)
    private val isRecording = AtomicBoolean(false)
    private var shouldSendEndFrame = false
    private var sdkInitRequested = false
    private var abilityListenerRegistered = false

    private val coreListener = CoreListener { type, code ->
        if (type == ErrType.AUTH) {
            emitEvent(
                type = "auth",
                message = if (code == 0) "SDK授权成功" else "SDK授权失败: $code",
                extras = mapOf("code" to code)
            )
        }
    }

    private val abilityListener = object : AiListener {
        override fun onResult(handleID: Int, outputData: List<AiResponse>?, usrContext: Any?) {
            if (outputData.isNullOrEmpty()) {
                return
            }
            for (resp in outputData) {
                val key = resp.key
                val result = String(resp.value)
                val status = resp.status
                emitEvent(
                    type = if (key == "func_wake_up" || key == "func_pre_wakeup") "wakeup" else "log",
                    message = "key=$key status=$status value=$result",
                    extras = mapOf("key" to key, "status" to status, "result" to result)
                )
            }
        }

        override fun onEvent(i: Int, i1: Int, list: List<AiResponse>?, o: Any?) {
            emitEvent(type = "log", message = "onEvent: $i, event=$i1")
        }

        override fun onError(i: Int, i1: Int, s: String?, o: Any?) {
            emitEvent(type = "error", message = "能力执行错误: code=$i1, msg=${s ?: ""}")
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        initIvwThread()

        // ========== 原有：电量调用逻辑 ==========
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, BATTERY_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getBatteryLevel" -> {
                    val batteryLevel = getBatteryLevel()
                    if (batteryLevel != -1) {
                        result.success(batteryLevel)
                    } else {
                        result.error("UNAVAILABLE", "电池信息不可用", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        // ========== 新增：WiFi信号强度调用逻辑 ==========
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, WIFI_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                // Flutter端调用的方法名：getWifiSignalStrength
                "getWifiSignalStrength" -> {
                    val signalStrength = getWifiSignalStrength()
                    if (signalStrength != -999) { // 自定义兜底值
                        result.success(signalStrength)
                    } else {
                        result.error("UNAVAILABLE", "WiFi信号强度获取失败", null)
                    }
                }
                // 可选：新增获取WiFi是否连接的方法
                "isWifiConnected" -> {
                    val isConnected = isWifiConnected()
                    result.success(isConnected)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, IVW_EVENT_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                }

                override fun onCancel(arguments: Any?) {
                    eventSink = null
                }
            })

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, IVW_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "initSdk" -> {
                    initSdk()
                    result.success(null)
                }

                "startRecordWake" -> {
                    val keyword = call.argument<String>("keyword") ?: "你好小迪"
                    if (!hasRecordPermission()) {
                        result.error("NO_PERMISSION", "缺少录音权限: RECORD_AUDIO", null)
                        return@setMethodCallHandler
                    }
                    ivwHandler.post {
                        val ret = startSession(keyword)
                        if (ret != 0) {
                            emitEvent(type = "error", message = "启动录音唤醒失败: $ret")
                            return@post
                        }
                        startRecordLoop()
                    }
                    result.success(null)
                }

                "stopRecordWake" -> {
                    ivwHandler.post {
                        if (isRecording.get()) {
                            shouldSendEndFrame = true
                        } else {
                            endSession()
                        }
                    }
                    result.success(null)
                }

                "startFileWake" -> {
                    val keyword = call.argument<String>("keyword") ?: "你好小迪"
                    val path = call.argument<String>("audioPath") ?: ""
                    if (path.isBlank()) {
                        result.error("INVALID_ARGS", "audioPath不能为空", null)
                        return@setMethodCallHandler
                    }
                    ivwHandler.post {
                        val ret = startSession(keyword)
                        if (ret != 0) {
                            emitEvent(type = "error", message = "启动文件唤醒失败: $ret")
                            return@post
                        }
                        audioPath = path
                        writeByFile()
                    }
                    result.success(null)
                }

                "releaseIvw" -> {
                    ivwHandler.post {
                        endSession()
                        unInitSdk()
                    }
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        }
    }

    // ========== 原有：获取电池电量 ==========
    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    // ========== 新增：获取WiFi信号强度（返回dBm值，负数，如-65） ==========
    private fun getWifiSignalStrength(): Int {
        // 1. 获取WifiManager实例
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        // 2. 检查WiFi是否开启
        if (!wifiManager.isWifiEnabled) {
            return -999 // 未开启WiFi，返回兜底值
        }

        // 3. 获取当前WiFi连接信息
        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 推荐用法
            wifiManager.connectionInfo
        } else {
            // 低版本兼容
            wifiManager.connectionInfo
        }

        // 4. 获取信号强度（RSSI：Received Signal Strength Indicator，单位dBm）
        return wifiInfo?.rssi ?: -999 // 未连接WiFi返回-999
    }

    // ========== 可选：判断WiFi是否已连接 ==========
    private fun isWifiConnected(): Boolean {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) return false

        val wifiInfo = wifiManager.connectionInfo
        // 判断是否已分配IP（已连接的标志）
        return wifiInfo.ipAddress != 0
    }

    private fun initIvwThread() {
        ivwHandlerThread = HandlerThread("ivw-worker")
        ivwHandlerThread.start()
        ivwHandler = Handler(ivwHandlerThread.looper)
    }

    private fun initSdk() {
        if (sdkInitRequested) {
            emitEvent(type = "log", message = "SDK已经初始化过，无需重复初始化")
            return
        }
        appId = getString(R.string.appId)
        apiKey = getString(R.string.apiKey)
        apiSecret = getString(R.string.apiSecret)
        workDir = getString(R.string.workDir)
        resDir = "${workDir}ivw"

        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, "${workDir}aikit/aeeLog.txt")
        val params = BaseLibrary.Params.builder()
            .appId(appId)
            .apiKey(apiKey)
            .apiSecret(apiSecret)
            .workDir(workDir)
            .build()
        Thread {
            AiHelper.getInst().initEntry(applicationContext, params)
        }.start()
        AiHelper.getInst().registerListener(coreListener)
        if (!abilityListenerRegistered) {
            AiHelper.getInst().registerListener(ABILITY_ID, abilityListener)
            abilityListenerRegistered = true
        }
        sdkInitRequested = true
        emitEvent(type = "log", message = "SDK初始化已发起")
    }

    private fun startSession(keywordInput: String): Int {
        if (!sdkInitRequested) {
            emitEvent(type = "error", message = "请先调用initSdk初始化")
            return -1
        }
        if (!keywordToFile(keywordInput)) {
            emitEvent(type = "error", message = "唤醒词写入失败，请检查workDir权限")
            return -2
        }
        val customBuilder = AiRequest.builder()
        customBuilder.customText("key_word", "$resDir/keyword.txt", 0)
        var ret = AiHelper.getInst().loadData(ABILITY_ID, customBuilder.build())
        if (ret != 0) {
            emitEvent(type = "error", message = "loadData失败: $ret")
            return ret
        }
        val indexes = intArrayOf(0)
        ret = AiHelper.getInst().specifyDataSet(ABILITY_ID, "key_word", indexes)
        if (ret != 0) {
            emitEvent(type = "error", message = "specifyDataSet失败: $ret")
            return ret
        }
        val paramBuilder = AiRequest.builder()
        paramBuilder.param("wdec_param_nCmThreshold", "0 0:800")
        paramBuilder.param("gramLoad", true)
        isEnd.set(false)
        aiHandle = AiHelper.getInst().start(ABILITY_ID, paramBuilder.build(), null)
        val code = aiHandle?.code ?: -1
        if (code != 0) {
            emitEvent(type = "error", message = "start失败: $code")
            return code
        }
        emitEvent(type = "state", message = "唤醒会话已开始", extras = mapOf("state" to "started"))
        return 0
    }

    private fun startRecordLoop() {
        createAudioRecordIfNeed()
        val recorder = audioRecord ?: run {
            emitEvent(type = "error", message = "录音器创建失败")
            return
        }
        shouldSendEndFrame = false
        isRecording.set(true)
        recorder.startRecording()
        emitEvent(type = "log", message = "开始录音送引擎")

        ivwHandler.post(object : Runnable {
            override fun run() {
                if (!isRecording.get()) {
                    return
                }
                val data = ByteArray(BUFFER_SIZE)
                val read = recorder.read(data, 0, BUFFER_SIZE)
                if (read > 0) {
                    val sendData = if (read == BUFFER_SIZE) data else data.copyOf(read)
                    val volume = calculateVolume(sendData)
                    emitEvent(
                        type = "db",
                        message = "当前分贝: ${kotlin.math.abs(volume)}",
                        extras = mapOf("db" to kotlin.math.abs(volume))
                    )
                    val status = if (shouldSendEndFrame) AiStatus.END else AiStatus.CONTINUE
                    write(sendData, status)
                    if (status == AiStatus.END) {
                        stopRecordInternal()
                        endSession()
                        return
                    }
                }
                ivwHandler.post(this)
            }
        })
    }

    private fun writeByFile() {
        try {
            val file = File(audioPath)
            if (!file.exists()) {
                emitEvent(type = "error", message = "音频文件不存在: $audioPath")
                endSession()
                return
            }
            val audioData = file.readBytes()
            var leftBytes = audioData.size
            var index = 0
            while (leftBytes > 0) {
                val writeLen = if (leftBytes > WRITE_CHUNK_SIZE) WRITE_CHUNK_SIZE else leftBytes
                leftBytes -= writeLen
                val part = Arrays.copyOfRange(
                    audioData,
                    index * WRITE_CHUNK_SIZE,
                    index * WRITE_CHUNK_SIZE + writeLen
                )
                val status = when {
                    index == 0 -> AiStatus.BEGIN
                    leftBytes == 0 -> AiStatus.END
                    else -> AiStatus.CONTINUE
                }
                write(part, status)
                index++
            }
            emitEvent(type = "log", message = "上传音频写入完成")
        } catch (e: Exception) {
            emitEvent(type = "error", message = "读取音频失败: ${e.message}")
        } finally {
            endSession()
        }
    }

    private fun write(part: ByteArray, status: AiStatus) {
        if (isEnd.get()) {
            return
        }
        val handle = aiHandle ?: return
        val builder = AiRequest.builder()
        val aiAudio = AiAudio.get("wav").data(part).status(status).valid()
        builder.payload(aiAudio)
        val ret = AiHelper.getInst().write(builder.build(), handle)
        if (ret != 0) {
            emitEvent(type = "error", message = "write失败: $ret")
        }
    }

    private fun endSession() {
        if (isEnd.get()) {
            emitEvent(type = "state", message = "会话已结束", extras = mapOf("state" to "stopped"))
            return
        }
        val handle = aiHandle ?: return
        val ret = AiHelper.getInst().end(handle)
        if (ret == 0) {
            isEnd.set(true)
            aiHandle = null
            emitEvent(type = "state", message = "唤醒结束 end=$ret", extras = mapOf("state" to "stopped"))
        } else {
            isEnd.set(false)
            emitEvent(type = "error", message = "end失败: $ret")
        }
    }

    private fun keywordToFile(keywordInput: String): Boolean {
        try {
            val dir = File(resDir)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val keywordFile = File("$resDir/keyword.txt")
            if (keywordFile.exists()) {
                keywordFile.delete()
            }
            val binFile = File("$resDir/keyword.bin")
            if (binFile.exists()) {
                binFile.delete()
            }
            val temp = if (keywordInput.isBlank()) "你好小迪" else keywordInput
            val normalized = temp.replace("，", ",")
            val keywords = normalized.split(",")
            if (!keywordFile.exists()) {
                keywordFile.createNewFile()
            }
            val writer = OutputStreamWriter(FileOutputStream(keywordFile), Charsets.UTF_8)
            val bufferedWriter = BufferedWriter(writer)
            for (item in keywords) {
                val kw = item.trim()
                if (kw.isNotEmpty()) {
                    bufferedWriter.write(kw)
                    bufferedWriter.write(";")
                    bufferedWriter.newLine()
                }
            }
            bufferedWriter.close()
            return true
        } catch (e: IOException) {
            emitEvent(type = "error", message = "关键词写文件失败: ${e.message}")
            return false
        }
    }

    private fun createAudioRecordIfNeed() {
        if (audioRecord != null) {
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            BUFFER_SIZE
        )
    }

    private fun stopRecordInternal() {
        isRecording.set(false)
        shouldSendEndFrame = false
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "audioRecord stop failed: ${e.message}")
        }
        emitEvent(type = "log", message = "录音已停止")
    }

    private fun calculateVolume(buffer: ByteArray): Int {
        var sumVolume = 0.0
        for (i in buffer.indices step 2) {
            if (i + 1 >= buffer.size) break
            val v1 = buffer[i].toInt() and 0xFF
            val v2 = buffer[i + 1].toInt() and 0xFF
            var temp = v1 + (v2 shl 8)
            if (temp >= 0x8000) {
                temp = 0xFFFF - temp
            }
            sumVolume += kotlin.math.abs(temp.toDouble())
        }
        val avgVolume = sumVolume / buffer.size / 2
        return (kotlin.math.log10(1 + avgVolume) * 10).toInt()
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun emitEvent(type: String, message: String, extras: Map<String, Any> = emptyMap()) {
        runOnUiThread {
            val payload = mutableMapOf<String, Any>("type" to type, "message" to message)
            payload.putAll(extras)
            eventSink?.success(payload)
        }
    }

    private fun unInitSdk() {
        try {
            AiHelper.getInst().unInit()
            sdkInitRequested = false
            emitEvent(type = "log", message = "SDK已逆初始化")
        } catch (e: Exception) {
            emitEvent(type = "error", message = "SDK逆初始化失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ivwHandler.post {
            stopRecordInternal()
            endSession()
        }
        audioRecord?.release()
        audioRecord = null
        ivwHandlerThread.quitSafely()
        unInitSdk()
    }
}