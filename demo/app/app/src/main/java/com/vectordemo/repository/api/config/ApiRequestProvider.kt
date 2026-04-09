package com.vectordemo.repository.api.config

import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.repository.api.ApiRequest
import com.vectordemo.utils.network.BaseApiRequestProvider
import com.vectordemo.utils.network.LoggingInterceptor
import com.vectordemo.utils.network.TimeoutInterceptor
import okhttp3.Interceptor

class ApiRequestProvider : BaseApiRequestProvider() {
    companion object {
        @Volatile
        private var apiRequest: ApiRequest? = null

        fun getApiRequest(): ApiRequest {
            return apiRequest ?: synchronized(this) {
                apiRequest ?: createApiRequest(
                    ApiRequest::class.java,
                    ApiUrlConfig.getUrl(),
                    BaseConstant.HttpConstant.CONNECT_TIMEOUT,
                    BaseConstant.HttpConstant.READ_TIMEOUT,
                    BaseConstant.HttpConstant.WRITE_TIMEOUT,
                    BaseConstant.HttpConstant.CALL_TIMEOUT,
                    getInterceptors()
                ).also { apiRequest = it }
            }
        }

        private fun getInterceptors(): List<Interceptor> = listOf(
            TimeoutInterceptor(),
            AuthInterceptor(),
            LoggingInterceptor(true)
        )
    }
}
