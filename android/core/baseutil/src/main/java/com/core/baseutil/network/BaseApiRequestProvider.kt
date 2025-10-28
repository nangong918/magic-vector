package com.core.baseutil.network

import androidx.multidex.BuildConfig
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

    companion object{
        // 创建 API 请求
        fun <T> createApiRequest(
            apiClass: Class<T>,
            mainUrl: String,
            connectTimeOut: Long,
            readTimeOut: Long,
            writeTimeOut: Long,
            callTimeOut: Long,
            interceptors: List<Interceptor>
        ): T {
            val uploadOkHttpClient = createUploadOkHttpClient(
                connectTimeOut,
                readTimeOut,
                writeTimeOut,
                callTimeOut,
                interceptors
            )

            return Retrofit.Builder()
                .baseUrl(mainUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(uploadOkHttpClient)
                .build()
                .create(apiClass) // 使用传入的 Class 对象
        }

        // 创建 OkHttpClient
        private fun createUploadOkHttpClient(
            connectTimeOut: Long,
            readTimeOut: Long,
            writeTimeOut: Long,
            callTimeOut: Long,
            interceptors: List<Interceptor>
        ): OkHttpClient {
            // 创建缓存目录
            val cacheFile = getCacheDir()
            val cache = Cache(cacheFile, 1024 * 1024 * 50) // 50MB 缓存大小

            // 创建日志拦截器实例
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY // 记录请求和响应的完整内容
                } else {
                    HttpLoggingInterceptor.Level.NONE // 不记录任何日志
                }
            }

            // 创建 OkHttpClient.Builder
            val builder = OkHttpClient.Builder()
                .retryOnConnectionFailure(false) // 不重复请求
                .connectTimeout(connectTimeOut, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS)
                .callTimeout(callTimeOut, TimeUnit.MILLISECONDS)
                .cache(cache)
                .addInterceptor(loggingInterceptor) // OkHttp3 日志拦截器
                .proxy(Proxy.NO_PROXY)

            // 添加传入的拦截器
            interceptors.forEach { builder.addInterceptor(it) }

            return builder.build()
        }

        // 自定义缓存目录
        private fun getCacheDir(): File {
            // 使用临时目录
            return File(System.getProperty("java.io.tmpdir"), "http-cache")
        }
    }


}