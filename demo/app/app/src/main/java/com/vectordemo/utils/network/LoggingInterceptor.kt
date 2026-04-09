package com.vectordemo.utils.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class LoggingInterceptor(private val showHeader: Boolean = true) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.d("LoggingInterceptor", "request: ${request.method} ${request.url}")
        if (showHeader) Log.d("LoggingInterceptor", "request headers: ${request.headers}")
        val response = chain.proceed(request)
        Log.d("LoggingInterceptor", "response: ${response.code} ${request.url}")
        if (showHeader) Log.d("LoggingInterceptor", "response headers: ${response.headers}")
        return response
    }
}
