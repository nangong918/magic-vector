package com.vectordemo.utils.network

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * [HttpLoggingInterceptor] 的 [HttpLoggingInterceptor.Logger]：对文件上传/内容更新等多部分请求，
 * 在请求头之后、直到 `--> END` 之前不打印原始二进制，避免 logcat 乱码与卡顿。
 */
class SkipMultipartBinaryHttpLogger(
    private val tag: String = "OkHttp"
) : HttpLoggingInterceptor.Logger {

    private enum class Phase { Normal, SkippingMultipartRequestBody }

    private var phase = Phase.Normal

    private fun isMultipartUploadRequestStart(line: String): Boolean {
        if (!line.startsWith("-->") || line.contains("--> END")) return false
        return line.contains("/oss/upload/batch") || line.contains("/oss/file/content/update")
    }

    override fun log(message: String) {
        when (phase) {
            Phase.Normal -> {
                if (isMultipartUploadRequestStart(message)) {
                    Log.d(tag, message)
                    phase = Phase.SkippingMultipartRequestBody
                } else {
                    Log.d(tag, message)
                }
            }

            Phase.SkippingMultipartRequestBody -> {
                when {
                    message.startsWith("--> END") -> {
                        Log.d(tag, "[request body omitted: multipart/binary not logged]")
                        Log.d(tag, message)
                        phase = Phase.Normal
                    }

                    message.startsWith("<--") -> {
                        Log.d(tag, "[request body omitted: multipart/binary not logged (no --> END seen)]")
                        Log.d(tag, message)
                        phase = Phase.Normal
                    }

                    message.startsWith("-->") -> {
                        Log.d(tag, "[request body omitted: multipart/binary not logged (new request line)]")
                        phase = Phase.Normal
                        log(message)
                    }

                    message.contains(':') || message.isBlank() -> Log.d(tag, message)

                    else -> Unit
                }
            }
        }
    }
}
