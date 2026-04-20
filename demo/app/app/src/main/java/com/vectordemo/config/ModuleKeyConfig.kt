package com.vectordemo.config

import android.content.Context
import org.json.JSONObject

data class XfLlmKeyConfig(
    val hostUrl: String,
    val appId: String,
    val apiKey: String,
    val apiSecret: String,
    val domain: String,
    val patchId: String
)

data class AliLlmKeyConfig(
    val hostUrl: String,
    val apiKey: String,
    val model: String,
    val temperature: Double,
    val maxTokens: Int
)

data class AliSttKeyConfig(
    val hostUrl: String,
    val apiKey: String,
    val model: String,
    val format: String,
    val sampleRate: Int,
    val disfluencyRemovalEnabled: Boolean,
    val languageHints: List<String>
)

data class OfflineIvwKeyConfig(
    val appId: String,
    val apiKey: String,
    val apiSecret: String,
    val abilityId: String
)

data class ModuleKeyConfig(
    val xfLlm: XfLlmKeyConfig,
    val aliLlm: AliLlmKeyConfig,
    val aliStt: AliSttKeyConfig,
    val offlineIvw: OfflineIvwKeyConfig
)

object ModuleKeyConfigStore {
    @Volatile
    private var cache: ModuleKeyConfig? = null

    fun load(context: Context): ModuleKeyConfig {
        val cached = cache
        if (cached != null) {
            return cached
        }
        val content = context.assets.open("module_key.json").bufferedReader().use { it.readText() }
        val root = JSONObject(content)

        val xfyun = root.optJSONObject("xfyun") ?: JSONObject()
        val llm = xfyun.optJSONObject("llm") ?: JSONObject()
        val offlineIvw = xfyun.optJSONObject("offlineIvw") ?: JSONObject()
        val sttAli = root.optJSONObject("stt_ali") ?: JSONObject()
        val llmAli = root.optJSONObject("llm_ali") ?: JSONObject()

        val config = ModuleKeyConfig(
            xfLlm = XfLlmKeyConfig(
                hostUrl = llm.optString("hostUrl"),
                appId = llm.optString("appId"),
                apiKey = llm.optString("apiKey"),
                apiSecret = llm.optString("apiSecret"),
                domain = llm.optString("domain"),
                patchId = llm.optString("patchId")
            ),
            aliLlm = AliLlmKeyConfig(
                hostUrl = llmAli.optString(
                    "hostUrl",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                ),
                apiKey = llmAli.optString("apiKey", sttAli.optString("apiKey")),
                model = llmAli.optString("model", "qwen-plus"),
                temperature = llmAli.optDouble("temperature", 0.5),
                maxTokens = llmAli.optInt("maxTokens", 4096)
            ),
            aliStt = AliSttKeyConfig(
                hostUrl = sttAli.optString("hostUrl", "wss://dashscope.aliyuncs.com/api-ws/v1/inference/"),
                apiKey = sttAli.optString("apiKey"),
                model = sttAli.optString("model", "paraformer-realtime-v2"),
                format = sttAli.optString("format", "pcm"),
                sampleRate = sttAli.optInt("sampleRate", 16000),
                disfluencyRemovalEnabled = sttAli.optBoolean("disfluencyRemovalEnabled", false),
                languageHints = buildList {
                    val array = sttAli.optJSONArray("languageHints")
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            add(array.optString(i))
                        }
                    }
                }
            ),
            offlineIvw = OfflineIvwKeyConfig(
                appId = offlineIvw.optString("appId"),
                apiKey = offlineIvw.optString("apiKey"),
                apiSecret = offlineIvw.optString("apiSecret"),
                abilityId = offlineIvw.optString("abilityId")
            )
        )
        cache = config
        return config
    }
}

