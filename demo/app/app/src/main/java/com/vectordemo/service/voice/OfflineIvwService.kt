package com.vectordemo.service.voice

import android.content.Context
import com.demo.aarlib.voicewakeup.VoiceWakeUpBridge
import com.vectordemo.config.ModuleKeyConfigStore
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class OfflineIvwEventType {
    LOG, DB, WAKEUP, AUTH, STATE, ERROR
}

data class OfflineIvwEvent(
    val type: OfflineIvwEventType,
    val message: String,
    val db: Int?,
    val raw: Map<String, Any?>
)

class OfflineIvwService(private val context: Context) {
    fun hasRecordPermission(): Boolean = VoiceWakeUpBridge.hasRecordPermission(context)

    fun init() {
        val cfg = ModuleKeyConfigStore.load(context).offlineIvw
        val workDir = resolveWritableWorkDir()
        require(cfg.abilityId.isNotBlank()) { "offlineIvw.abilityId 不能为空，请在 module_key.json 配置" }
        VoiceWakeUpBridge.setDefaultConfig(
            VoiceWakeUpBridge.Config(
                cfg.appId,
                cfg.apiKey,
                cfg.apiSecret,
                workDir,
                cfg.abilityId
            )
        )
        VoiceWakeUpBridge.initSdk(context)
    }

    fun buildInitDiagnostics(): String {
        val cfg = ModuleKeyConfigStore.load(context).offlineIvw
        val workDir = resolveWritableWorkDir()
        val keywordAssetExists = runCatching {
            context.assets.open("ivw/keyword1.txt").use { true }
        }.getOrDefault(false)
        val aikitAssetExists = runCatching {
            context.assets.list("aikit_resources")?.isNotEmpty() == true
        }.getOrDefault(false)
        return "IVW诊断: abilityId=${cfg.abilityId}, workDir=$workDir, workDirExists=${File(workDir).exists()}, " +
            "keywordAsset=$keywordAssetExists, aikitResources=$aikitAssetExists"
    }

    fun startRecordWake(keyword: String) {
        VoiceWakeUpBridge.startRecordWake(context, keyword)
    }

    fun stopRecordWake() {
        VoiceWakeUpBridge.stopRecordWake()
    }

    fun release() {
        VoiceWakeUpBridge.release()
    }

    fun events(): Flow<OfflineIvwEvent> = callbackFlow {
        VoiceWakeUpBridge.setEventListener { payload ->
            val map = payload.mapValues { it.value }
            val type = parseType(map["type"]?.toString().orEmpty())
            val db = (map["db"] as? Number)?.toInt()
            val event = OfflineIvwEvent(
                type = type,
                message = map["message"]?.toString().orEmpty(),
                db = db,
                raw = map
            )
            trySend(event)
        }
        awaitClose { VoiceWakeUpBridge.clearEventListener() }
    }

    private fun parseType(type: String): OfflineIvwEventType {
        return when (type) {
            "db" -> OfflineIvwEventType.DB
            "wakeup" -> OfflineIvwEventType.WAKEUP
            "auth" -> OfflineIvwEventType.AUTH
            "state" -> OfflineIvwEventType.STATE
            "error" -> OfflineIvwEventType.ERROR
            else -> OfflineIvwEventType.LOG
        }
    }

    private fun resolveWritableWorkDir(): String {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val iflytekDir = File(baseDir, "iflytek")
        if (!iflytekDir.exists()) {
            iflytekDir.mkdirs()
        }
        val absolute = iflytekDir.absolutePath
        return if (absolute.endsWith(File.separator)) {
            absolute
        } else {
            "$absolute${File.separator}"
        }
    }
}

