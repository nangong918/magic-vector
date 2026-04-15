package com.vectordemo.utils.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.nio.charset.StandardCharsets

/**
 * 仅当日志内容为 JSON 时打印 body；multipart、二进制等不读取、不输出正文（参考 magicvector [LoggingInterceptor] 思路）。
 * 与 [HttpLoggingInterceptor]（建议 [okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS]）配合使用。
 */
class JsonOrOmitBodyLoggingInterceptor(
    private val tag: String = "OkHttp",
    private val showHeaders: Boolean = true
) : Interceptor {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        logRequestStart(original)
        val requestToSend = maybeRebuildRequestForJsonLog(original)
        val response = chain.proceed(requestToSend)
        return logResponseBodyAndRebuild(response)
    }

    private fun logRequestStart(request: Request) {
        Log.d(tag, "--> ${request.method} ${request.url}")
        if (showHeaders) Log.d(tag, "Request headers: ${request.headers}")
    }

    private fun maybeRebuildRequestForJsonLog(request: Request): Request {
        val body = request.body ?: return request
        val media = body.contentType()
        val ct = media?.toString().orEmpty()
        if (!isJsonContentType(ct)) {
            Log.d(tag, "request body: (omitted, non-JSON Content-Type: $ct)")
            return request
        }
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = media?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            val raw = buffer.readString(charset)
            Log.i(tag, "request JSON body:\n${formatJsonIfPossible(raw)}")
            val newBody = raw.toRequestBody(media ?: "application/json; charset=utf-8".toMediaTypeOrNull())
            request.newBuilder().method(request.method, newBody).build()
        } catch (e: Exception) {
            Log.w(tag, "request body: (omitted, failed to read JSON body)", e)
            request
        }
    }

    private fun logResponseBodyAndRebuild(response: Response): Response {
        val body = response.body
        val raw = try {
            val source = body.source()
            source.request(Long.MAX_VALUE)
            val buf = source.buffer
            val snapshot = buf.clone()
            val byteCount = minOf(snapshot.size, MAX_PEEK_BYTES)
            if (snapshot.size > MAX_PEEK_BYTES) {
                Log.d(tag, "response body log truncated to ${MAX_PEEK_BYTES}B")
            }
            snapshot.readUtf8(byteCount)
        } catch (e: Exception) {
            Log.w(tag, "response body: (omitted, read failed)", e)
            return response
        }
        val ct = body.contentType()?.toString().orEmpty()
        if (isJsonContentType(ct)) {
            Log.i(tag, "<-- ${response.code} ${response.request.url}\nresponse JSON body:\n${formatJsonIfPossible(raw)}")
        } else {
            Log.d(tag, "<-- ${response.code} ${response.request.url}\nresponse body: (omitted, non-JSON Content-Type: $ct)")
        }
        if (showHeaders) Log.d(tag, "Response headers: ${response.headers}")
        return response
    }

    private fun isJsonContentType(ct: String): Boolean =
        ct.contains("application/json", ignoreCase = true) ||
            ct.contains("application/*+json", ignoreCase = true)

    private fun formatJsonIfPossible(raw: String): String {
        if (raw.isBlank()) return raw
        return try {
            val el = gson.fromJson(raw, Any::class.java)
            gson.toJson(el)
        } catch (_: Exception) {
            raw
        }
    }

    companion object {
        private const val MAX_PEEK_BYTES = 512L * 1024L
    }
}
