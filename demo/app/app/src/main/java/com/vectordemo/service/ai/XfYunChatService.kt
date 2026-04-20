package com.vectordemo.service.ai

import android.util.Base64
import com.vectordemo.config.XfLlmKeyConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class XfYunChatService(
    private val configProvider: () -> XfLlmKeyConfig
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
        val wsUrl = buildAuthenticatedWsUrl(cfg)
        val done = CompletableDeferred<Unit>()
        var doneNotified = false

        fun notifyDone() {
            if (!doneNotified) {
                doneNotified = true
                onDone()
            }
        }

        val request = Request.Builder().url(wsUrl).build()
        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val body = buildRequestBody(cfg, systemPrompt, history, userMessage)
                webSocket.send(body.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val status = handleEvent(text, onDelta)
                if (status == 2 && !done.isCompleted) {
                    notifyDone()
                    done.complete(Unit)
                    webSocket.close(1000, "done")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!done.isCompleted) {
                    done.completeExceptionally(t)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!done.isCompleted) {
                    done.complete(Unit)
                }
            }
        })

        try {
            done.await()
        } finally {
            notifyDone()
            webSocket.close(1000, null)
        }
    }

    private fun buildRequestBody(
        cfg: XfLlmKeyConfig,
        systemPrompt: String,
        history: List<Map<String, String>>,
        userMessage: String
    ): JSONObject {
        val textArray = JSONArray()
        if (systemPrompt.isNotBlank()) {
            textArray.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        history.forEach {
            textArray.put(
                JSONObject()
                    .put("role", it["role"].orEmpty())
                    .put("content", it["content"].orEmpty())
            )
        }
        textArray.put(JSONObject().put("role", "user").put("content", userMessage))

        val header = JSONObject()
            .put("app_id", cfg.appId)
            .put("uid", buildUid())
        if (cfg.patchId.isNotBlank()) {
            header.put("patch_id", JSONArray().put(cfg.patchId))
        }

        return JSONObject()
            .put("header", header)
            .put(
                "parameter",
                JSONObject().put(
                    "chat",
                    JSONObject()
                        .put("domain", cfg.domain)
                        .put("temperature", 0.5)
                        .put("max_tokens", 4096)
                )
            )
            .put(
                "payload",
                JSONObject().put("message", JSONObject().put("text", textArray))
            )
    }

    private fun handleEvent(event: String, onDelta: (String) -> Unit): Int {
        val json = JSONObject(event)
        val header = json.optJSONObject("header") ?: JSONObject()
        val code = header.optInt("code", -1)
        if (code != 0) {
            throw IllegalStateException("XfYun chat error: code=$code sid=${header.optString("sid")}")
        }
        val choices = json.optJSONObject("payload")
            ?.optJSONObject("choices")
            ?.optJSONArray("text")
            ?: JSONArray()
        for (i in 0 until choices.length()) {
            val content = choices.optJSONObject(i)?.optString("content").orEmpty()
            if (content.isNotEmpty()) {
                onDelta(content)
            }
        }
        return header.optInt("status", 0)
    }

    private fun buildUid(): String {
        val millis = System.currentTimeMillis().toString()
        return if (millis.length > 10) millis.takeLast(10) else millis
    }

    private fun buildAuthenticatedWsUrl(cfg: XfLlmKeyConfig): String {
        val uri = URI(cfg.hostUrl)
        val date = rfc1123Date()
        val requestPath = if (uri.rawPath.isNullOrBlank()) "/" else uri.rawPath
        val signatureOrigin = "host: ${uri.host}\n" +
            "date: $date\n" +
            "GET $requestPath HTTP/1.1"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(cfg.apiSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = Base64.encodeToString(
            mac.doFinal(signatureOrigin.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
        val authorization =
            "api_key=\"${cfg.apiKey}\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val authorizationBase64 = Base64.encodeToString(
            authorization.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        val scheme = if (uri.scheme == "wss") "https" else "http"
        val host = uri.host
        val portPart = if (uri.port > 0) ":${uri.port}" else ""
        val query = "authorization=$authorizationBase64&date=${java.net.URLEncoder.encode(date, "UTF-8")}&host=$host"
        val authUrl = "$scheme://$host$portPart$requestPath?$query"
        return authUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
    }

    private fun rfc1123Date(): String {
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("GMT")
        return format.format(Date())
    }
}

