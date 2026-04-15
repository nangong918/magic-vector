package com.vectordemo.utils.network

import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.net.Proxy
import java.util.concurrent.TimeUnit

open class BaseApiRequestProvider {
    companion object {
        fun <T> createApiRequest(
            apiClass: Class<T>,
            mainUrl: String,
            connectTimeOut: Long,
            readTimeOut: Long,
            writeTimeOut: Long,
            callTimeOut: Long,
            interceptors: List<Interceptor>
        ): T {
            val client = createUploadOkHttpClient(connectTimeOut, readTimeOut, writeTimeOut, callTimeOut, interceptors)
            return Retrofit.Builder()
                .baseUrl(mainUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(apiClass)
        }

        private fun createUploadOkHttpClient(
            connectTimeOut: Long,
            readTimeOut: Long,
            writeTimeOut: Long,
            callTimeOut: Long,
            interceptors: List<Interceptor>
        ): OkHttpClient {
            val cache = Cache(File(System.getProperty("java.io.tmpdir"), "http-cache"), 50L * 1024 * 1024)
            val headerLog = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger.DEFAULT).apply {
                // 因为这个拦截器会拦截文件资源请求，所以取消掉BODY，只拦截HEADER，然后用JsonOrOmitBodyLoggingInterceptor拦截
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            val builder = OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .connectTimeout(connectTimeOut, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS)
                .callTimeout(callTimeOut, TimeUnit.MILLISECONDS)
                .cache(cache)
                .addInterceptor(JsonOrOmitBodyLoggingInterceptor())
                .addInterceptor(headerLog)
                .proxy(Proxy.NO_PROXY)
            interceptors.forEach { builder.addInterceptor(it) }
            return builder.build()
        }
    }
}
