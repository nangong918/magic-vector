package com.vectordemo.service.ai

import com.vectordemo.domain.config.AliLlmKeyConfig
import com.vectordemo.domain.config.XfLlmKeyConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
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
        println("[AliChatService] POST ${cfg.hostUrl} model=${cfg.model} stream=true")

        httpClient.preparePost(cfg.hostUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${cfg.apiKey}")
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }.execute { response ->
            if (response.status.value !in 200..299) {
                val errBody = response.bodyAsText()
                println("[AliChatService] HTTP ${response.status.value} body=$errBody")
                throw IllegalStateException("Ali LLM请求失败: ${response.status.value} $errBody")
            }

            val channel = response.bodyAsChannel()
            var done = false
            var lineCount = 0
            println("[AliChatService] SSE stream opened")

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                lineCount++
                if (!trimmed.startsWith("data:")) {
                    if (trimmed.startsWith("{")) {
                        println("[AliChatService] non-SSE JSON line#$lineCount len=${trimmed.length}")
                        emitFromPayload(trimmed, onDelta)?.let { finished ->
                            if (finished) {
                                done = true
                                onDone()
                                break
                            }
                        }
                    }
                    continue
                }

                val payloadLine = trimmed.removePrefix("data:").trim()
                if (payloadLine.isEmpty()) continue
                if (payloadLine == "[DONE]") {
                    println("[AliChatService] SSE [DONE]")
                    done = true
                    onDone()
                    break
                }

                val finished = emitFromPayload(payloadLine, onDelta)
                if (finished == true) {
                    println("[AliChatService] SSE finish_reason received")
                    done = true
                    onDone()
                    break
                }
            }

            if (!done) {
                println("[AliChatService] SSE stream ended without [DONE], lines=$lineCount")
                onDone()
            }
        }
    }

    /**
     * @return true 表示流已结束（finish_reason）
     */
    private fun emitFromPayload(payload: String, onDelta: (String) -> Unit): Boolean? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrElse {
            println("[AliChatService] JSON parse failed: ${it.message} payload=${payload.take(200)}")
            return null
        }
        root["error"]?.let { error ->
            throw IllegalStateException("Ali LLM返回错误: $error")
        }
        val choices = root["choices"]?.jsonArray ?: JsonArray(emptyList())
        if (choices.isEmpty()) return null

        val choice = choices.firstOrNull()?.jsonObject ?: return null
        val deltaText = choice["delta"]?.jsonObject?.get("content")?.jsonPrimitive?.content.orEmpty()
        val messageText = choice["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content.orEmpty()
        val text = deltaText.ifBlank { messageText }
        if (text.isNotEmpty()) {
            println("[AliChatService] onDelta chunk len=${text.length} preview=${text.take(80)}")
            onDelta(text)
        }

        val finishReason = choice["finish_reason"]?.jsonPrimitive?.content.orEmpty()
        return finishReason.isNotEmpty() && finishReason != "null"
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
            put("stream", JsonPrimitive(true))
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
