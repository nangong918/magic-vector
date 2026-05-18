package com.vectordemo.service.ai

import com.vectordemo.domain.config.AliLlmKeyConfig
import com.vectordemo.domain.config.XfLlmKeyConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ChatService {
    suspend fun sendChat(
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String,
        onDelta: (String) -> Unit,
        onDone: () -> Unit,
    )
}

class AliChatService(
    private val configProvider: () -> AliLlmKeyConfig,
    private val httpClient: HttpClient,
) : ChatService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun sendChat(
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String,
        onDelta: (String) -> Unit,
        onDone: () -> Unit,
    ) {
        val cfg = configProvider()
        require(cfg.hostUrl.isNotBlank()) {
            "llm_ali.hostUrl 未配置，请检查 module_key.json"
        }
        require(cfg.apiKey.isNotBlank()) {
            "llm_ali.apiKey 未配置，请检查 module_key.json"
        }
        val payload = buildAliPayload(cfg, systemPrompt, history, userMessage)
        val text = httpClient.post(cfg.hostUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${cfg.apiKey}")
            setBody(payload.toString())
        }.bodyAsText()
        val root = json.parseToJsonElement(text).jsonObject
        val choices = root["choices"]?.jsonArray ?: JsonArray(emptyList())
        val content = choices.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            .orEmpty()
        if (content.isNotBlank()) onDelta(content)
        onDone()
    }

    private fun buildAliPayload(
        cfg: AliLlmKeyConfig,
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String,
    ): JsonObject {
        return buildJsonObject {
            put("model", JsonPrimitive(cfg.model))
            put("temperature", JsonPrimitive(cfg.temperature))
            put("max_tokens", JsonPrimitive(cfg.maxTokens))
            put("stream", JsonPrimitive(false))
            put(
                "messages",
                buildJsonArray {
                    if (systemPrompt.isNotBlank()) {
                        add(jsonMessage("system", systemPrompt.trim()))
                    }
                    history.forEach { item ->
                        val content = item["content"].orEmpty().trim()
                        if (content.isBlank()) return@forEach
                        val role = item["role"].orEmpty().lowercase().let {
                            if (it == "system" || it == "assistant" || it == "user") it else "user"
                        }
                        add(jsonMessage(role, content))
                    }
                    add(jsonMessage("user", userMessage))
                },
            )
        }
    }

    private fun jsonMessage(role: String, content: String): JsonObject {
        return buildJsonObject {
            put("role", JsonPrimitive(role))
            put("content", JsonPrimitive(content))
        }
    }
}

class XfYunChatService(
    private val configProvider: () -> XfLlmKeyConfig,
    private val aliFallback: AliChatService,
) : ChatService {
    override suspend fun sendChat(
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String,
        onDelta: (String) -> Unit,
        onDone: () -> Unit,
    ) {
        val cfg = configProvider()
        if (cfg.hostUrl.startsWith("ws")) {
            throw IllegalStateException("KMP版本暂未接入讯飞WebSocket流式SDK，请在Android原生通道实现。")
        }
        aliFallback.sendChat(systemPrompt, history, userMessage, onDelta, onDone)
    }
}
