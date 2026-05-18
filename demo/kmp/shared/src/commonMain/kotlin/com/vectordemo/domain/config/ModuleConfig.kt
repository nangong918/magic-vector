package com.vectordemo.domain.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class XfLlmKeyConfig(
    val hostUrl: String = "",
    val appId: String = "",
    val apiKey: String = "",
    val apiSecret: String = "",
    val domain: String = "",
    val patchId: String = "",
)

@Serializable
data class AliLlmKeyConfig(
    val hostUrl: String = "",
    val apiKey: String = "",
    val model: String = "qwen-plus",
    val temperature: Double = 0.5,
    val maxTokens: Int = 4096,
)

@Serializable
data class ModuleXfConfig(
    val llm: XfLlmKeyConfig = XfLlmKeyConfig(),
)

@Serializable
data class ModuleAliSttConfig(
    val hostUrl: String = "",
    val apiKey: String = "",
)

@Serializable
data class ModuleKeyConfig(
    val xfyun: ModuleXfConfig = ModuleXfConfig(),
    @SerialName("llm_ali")
    val llmAli: AliLlmKeyConfig = AliLlmKeyConfig(),
    @SerialName("stt_ali")
    val sttAli: ModuleAliSttConfig = ModuleAliSttConfig(),
)

private const val DEFAULT_MODULE_KEY_JSON = """
{
  "xfyun": {
    "llm": {
      "hostUrl": "",
      "appId": "",
      "apiKey": "",
      "apiSecret": "",
      "domain": "",
      "patchId": ""
    }
  },
  "stt_ali": {
    "hostUrl": "",
    "apiKey": ""
  },
  "llm_ali": {
    "hostUrl": "",
    "apiKey": "",
    "model": "qwen-plus",
    "temperature": 0.5,
    "maxTokens": 4096
  }
}
"""

object ModuleKeyConfigStore {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun loadFromJson(content: String = DEFAULT_MODULE_KEY_JSON): ModuleKeyConfig {
        return runCatching { json.decodeFromString<ModuleKeyConfig>(content) }
            .getOrElse { ModuleKeyConfig() }
    }
}
