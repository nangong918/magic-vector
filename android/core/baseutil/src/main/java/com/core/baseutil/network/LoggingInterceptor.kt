package com.core.baseutil.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody
import okio.IOException
import okio.Buffer

class LoggingInterceptor(private val isShowHeader: Boolean = true) : Interceptor {

    companion object {
        private val TAG = LoggingInterceptor::class.java.name

        // 使用 GsonBuilder 创建格式化的 Gson 实例
        val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    }


    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // 打印请求信息
        Log.d(TAG, "\n\n----> Request")
        Log.d(TAG, "Request URL: ${request.url}")
        Log.d(TAG, "Method: ${request.method}")

        if (isShowHeader) {
            Log.d(TAG, "Request Headers: ${request.headers}")
        }

        val requestBodyString = request.body?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            buffer.readUtf8()
        }

        requestBodyString?.let { bodyString ->
            Log.d(TAG, "Request Content Type: ${request.body?.contentType()}")
            Log.i(TAG, "\n\n----> RequestBody -> \n${
                getJsonObject(bodyString, request.body?.contentType())
            }")
        } ?: Log.d(TAG, "request.body() == null")


        val response = chain.proceed(request)
        val endTime = System.nanoTime()

        // 打印响应信息
        Log.d(TAG, "\n\n<---- Response (${(endTime - startTime) / 1e6}ms)")
        Log.d(TAG, "Response URL: ${request.url}") // 打印请求的 URL
        Log.d(TAG, "Status Code: ${response.code}")

        if (isShowHeader) {
            Log.d(TAG, "Response Headers: ${response.headers}")
        }

        val responseBody = response.body
        val responseBodyString = responseBody.string()


        Log.i(TAG, "\n\n<---- ResponseBody: \n${
            getJsonObject(responseBodyString, response.headers)
        }")

        return response.newBuilder()
            .body(ResponseBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), responseBodyString))
            .build()
    }

    private fun getJsonObject(jsonString: String, headers: Headers?): String {
        val contentType = headers?.get("Content-Type")
        val isJsonRequest = contentType?.contains("application/json") == true

        return if (isJsonRequest) {
            try {
                val jsonObject = GSON.fromJson(jsonString, Any::class.java)
                GSON.toJson(jsonObject)
            } catch (e: Exception) {
                jsonString // 返回原始字符串
            }
        } else {
            Log.e(TAG, "Non-JSON content") // 返回非 JSON 内容提示
            ""
        }
    }

    private fun getJsonObject(jsonString: String, mediaType: MediaType?): String {
        val contentType = mediaType?.toString()
        val isJsonRequest = contentType?.contains("application/json") == true

        return if (isJsonRequest) {
            try {
                val jsonObject = Gson().fromJson(jsonString, Any::class.java)
                Gson().toJson(jsonObject)
            } catch (e: Exception) {
                jsonString // 返回原始字符串
            }
        } else {
            Log.e(TAG, "Non-JSON content") // 返回非 JSON 内容提示
            ""
        }
    }
}