package com.vectordemo.domain.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class XfLlmKeyConfig(
    val hostUrl: String = "",
    val appId: String = "",
    val apiKey: String = "",
    val apiSecret: String = "",
    val domain: String = "",
    val patchId: String = "",
)

data class AliLlmKeyConfig(
    val hostUrl: String = "",
    val apiKey: String = "",
    val model: String = "qwen-plus",
    val temperature: Double = 0.5,
    val maxTokens: Int = 4096,
)

data class ModuleXfConfig(
    val llm: XfLlmKeyConfig = XfLlmKeyConfig(),
)

data class ModuleAliSttConfig(
    val hostUrl: String = "",
    val apiKey: String = "",
)

data class ModuleKeyConfig(
    val xfyun: ModuleXfConfig = ModuleXfConfig(),
    val llmAli: AliLlmKeyConfig = AliLlmKeyConfig(),
    val sttAli: ModuleAliSttConfig = ModuleAliSttConfig(),
)

object ModuleKeyConfigStore {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun loadFromJson(content: String): ModuleKeyConfig {
        require(content.isNotBlank()) { "module_key.json 内容为空" }
        val moduleKeyConfig = parseModuleKeyConfig(content)
        // 输出模块配置
        println("ai模块配置: $moduleKeyConfig")
        return moduleKeyConfig
    }

    private fun parseModuleKeyConfig(content: String): ModuleKeyConfig {
        val root = json.parseToJsonElement(content).jsonObject
        val xfyun = root["xfyun"]?.jsonObject ?: JsonObject(emptyMap())
        val llm = xfyun["llm"]?.jsonObject ?: JsonObject(emptyMap())
        val sttAliJson = root["stt_ali"]?.jsonObject ?: JsonObject(emptyMap())
        val llmAliJson = root["llm_ali"]?.jsonObject ?: JsonObject(emptyMap())

        val sttAli = ModuleAliSttConfig(
            hostUrl = sttAliJson.string("hostUrl"),
            apiKey = sttAliJson.string("apiKey"),
        )
        val llmAli = AliLlmKeyConfig(
            hostUrl = llmAliJson.string(
                "hostUrl",
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            ),
            apiKey = llmAliJson.string("apiKey").ifBlank { sttAli.apiKey },
            model = llmAliJson.string("model", "qwen-plus"),
            temperature = llmAliJson.double("temperature", 0.5),
            maxTokens = llmAliJson.int("maxTokens", 4096),
        )

        return ModuleKeyConfig(
            xfyun = ModuleXfConfig(
                llm = XfLlmKeyConfig(
                    hostUrl = llm.string("hostUrl"),
                    appId = llm.string("appId"),
                    apiKey = llm.string("apiKey"),
                    apiSecret = llm.string("apiSecret"),
                    domain = llm.string("domain"),
                    patchId = llm.string("patchId"),
                ),
            ),
            llmAli = llmAli,
            sttAli = sttAli,
        )
    }

    private fun JsonObject.string(key: String, default: String = ""): String {
        return this[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: default
    }

    private fun JsonObject.double(key: String, default: Double): Double {
        return this[key]?.jsonPrimitive?.doubleOrNull ?: default
    }

    private fun JsonObject.int(key: String, default: Int): Int {
        return this[key]?.jsonPrimitive?.intOrNull ?: default
    }
}
