package com.vectordemo.service.ai

import com.vectordemo.config.ModuleKeyConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AliChatService(
    private val configProvider: () -> com.vectordemo.config.AliLlmKeyConfig
) : ChatService {

    private val client = OkHttpClient.Builder().build()

    override suspend fun sendChat(
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String,
        onDelta: (String) -> Unit,
        onDone: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val cfg = configProvider()
        val body = buildBody(cfg, systemPrompt, history, userMessage)
        val request = Request.Builder()
            .url(cfg.hostUrl)
            .addHeader("Authorization", "Bearer ${cfg.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Ali LLM请求失败: ${response.code} ${response.body?.string().orEmpty()}")
            }
            val source = response.body?.source() ?: return@use
            var done = false
            while (!source.exhausted()) {
                val line = source.readUtf8Line()?.trim().orEmpty()
                if (!line.startsWith("data:")) {
                    continue
                }
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty()) {
                    continue
                }
                if (payload == "[DONE]") {
                    done = true
                    onDone()
                    break
                }
                val json = JSONObject(payload)
                if (json.has("error")) {
                    throw IllegalStateException("Ali LLM返回错误: ${json.get("error")}")
                }
                val choices = json.optJSONArray("choices") ?: JSONArray()
                if (choices.length() <= 0) {
                    continue
                }
                val choice = choices.optJSONObject(0) ?: continue
                val delta = choice.optJSONObject("delta")
                val text = delta?.optString("content").orEmpty()
                if (text.isNotEmpty()) {
                    onDelta(text)
                }
                val finishReason = choice.optString("finish_reason")
                if (finishReason.isNotEmpty() && finishReason != "null") {
                    done = true
                    onDone()
                    break
                }
            }
            if (!done) {
                onDone()
            }
        }
    }

    private fun buildBody(
        cfg: com.vectordemo.config.AliLlmKeyConfig,
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String
    ): JSONObject {
        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt.trim()))
        }
        history.forEach { item ->
            val content = item["content"].orEmpty().trim()
            if (content.isBlank()) return@forEach
            val role = item["role"].orEmpty().lowercase().let {
                if (it == "system" || it == "assistant" || it == "user") it else "user"
            }
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userMessage))
        return JSONObject()
            .put("model", cfg.model)
            .put("messages", messages)
            .put("stream", true)
            .put("temperature", cfg.temperature)
            .put("max_tokens", cfg.maxTokens)
    }
}

