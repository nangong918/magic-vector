package com.core.appcore.api

import com.core.baseutil.network.BaseApiRequestProvider
import com.core.baseutil.network.LoggingInterceptor
import com.core.baseutil.network.TimeoutInterceptor
import com.data.domain.constant.BaseConstant
import okhttp3.Interceptor


class ApiRequestProvider : BaseApiRequestProvider() {

    companion object {
        @Volatile
        private var apiRequest: ApiRequest? = null

        fun getApiRequest(): ApiRequest {
            return apiRequest ?: synchronized(this) {
                apiRequest ?: createApiRequest(
                    ApiRequest::class.java,
                    ApiUrlConfig.getMainUrl(),
                    BaseConstant.HttpConstant.CONNECT_TIMEOUT,
                    BaseConstant.HttpConstant.READ_TIMEOUT,
                    BaseConstant.HttpConstant.WRITE_TIMEOUT,
                    BaseConstant.HttpConstant.CALL_TIMEOUT,
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