package com.core.appcore.api

import com.core.baseutil.network.BaseApiRequestProvider
import com.core.baseutil.network.LoggingInterceptor
import com.core.baseutil.network.TimeoutInterceptor
import okhttp3.Interceptor


class ApiRequestProvider : BaseApiRequestProvider() {

    companion object {
        @Volatile
        private var apiRequest: ApiRequest? = null

        private const val CONNECT_TIMEOUT = 10L
        private const val READ_TIMEOUT = 10L
        private const val WRITE_TIMEOUT = 10L
        // 响应处理超时时间：30秒
        private const val CALL_TIMEOUT = 30L

        fun getApiRequest(): ApiRequest {
            return apiRequest ?: synchronized(this) {
                apiRequest ?: createApiRequest(
                    ApiRequest::class.java,
                    ApiUrlConfig.getMainUrl(),
                    CONNECT_TIMEOUT,
                    READ_TIMEOUT,
                    WRITE_TIMEOUT,
                    CALL_TIMEOUT,
                    getInterceptors()
                ).also { apiRequest = it } // 使用 also 更新 apiRequest
            }
        }

        private fun getInterceptors(): List<Interceptor> {
            return mutableListOf<Interceptor>().apply {
                // 超时拦截器
                add(TimeoutInterceptor())
                // 认证拦截器
//                add(ApiRequestProvider.getAuthInterceptor(null))
                // 日志拦截器
                add(LoggingInterceptor(true))
            }
        }
    }

}