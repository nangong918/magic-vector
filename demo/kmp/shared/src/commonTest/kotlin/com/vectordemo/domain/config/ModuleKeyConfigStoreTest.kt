package com.vectordemo.domain.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleKeyConfigStoreTest {

    @Test
    fun loadFromJson_parsesLlmAliApiKey() {
        val json = """
            {
              "xfyun": { "llm": { "hostUrl": "", "appId": "", "apiKey": "", "apiSecret": "", "domain": "", "patchId": "" } },
              "stt_ali": { "hostUrl": "", "apiKey": "stt-key" },
              "llm_ali": {
                "hostUrl": "https://example.com/chat",
                "apiKey": "llm-key",
                "model": "qwen-plus",
                "temperature": 0.5,
                "maxTokens": 4096
              }
            }
        """.trimIndent()

        val config = ModuleKeyConfigStore.loadFromJson(json)

        assertEquals("https://example.com/chat", config.llmAli.hostUrl)
        assertEquals("llm-key", config.llmAli.apiKey)
    }

    @Test
    fun loadFromJson_fallsBackToSttAliApiKey() {
        val json = """
            {
              "xfyun": { "llm": {} },
              "stt_ali": { "apiKey": "shared-key" },
              "llm_ali": { "hostUrl": "https://example.com/chat" }
            }
        """.trimIndent()

        val config = ModuleKeyConfigStore.loadFromJson(json)

        assertEquals("shared-key", config.llmAli.apiKey)
        assertTrue(config.llmAli.hostUrl.isNotBlank())
    }
}
