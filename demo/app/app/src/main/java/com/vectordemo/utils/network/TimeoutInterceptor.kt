package com.vectordemo.utils.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class TimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: IOException) {
            Log.e("TimeoutInterceptor", "request timeout: ${chain.request().url}", e)
            throw e
        }
    }
}
