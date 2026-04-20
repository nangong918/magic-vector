package com.vectordemo.service.voice

import android.content.Context
import com.demo.aarlib.voicewakeup.VoiceWakeUpBridge
import com.vectordemo.config.ModuleKeyConfigStore
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
        VoiceWakeUpBridge.setDefaultConfig(
            VoiceWakeUpBridge.Config(
                cfg.appId,
                cfg.apiKey,
                cfg.apiSecret,
                "",
                cfg.abilityId
            )
        )
        VoiceWakeUpBridge.initSdk(context)
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
}

