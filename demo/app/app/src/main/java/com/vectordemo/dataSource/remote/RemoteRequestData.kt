package com.vectordemo.dataSource.remote

import com.vectordemo.MainApplication
import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.domain.exception.NetworkBusinessException
import com.vectordemo.utils.auth.AuthTokenHandler
import com.vectordemo.utils.network.BaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object RemoteRequestData {
    suspend fun <T> requestData(
        apiCall: suspend () -> BaseResponse<T>,
        emptyDataMessage: String = "响应数据为空",
    ): T = withContext(Dispatchers.IO) {
        val response = apiCall()
        if (BaseConstant.NetworkCode.SUCCESS_CODE != response.code) {
            if (AuthTokenHandler.isTokenExpiredCode(response.code ?: "")) {
                AuthTokenHandler.handleTokenExpired(MainApplication.getApp())
            }
            throw NetworkBusinessException(response.code, response.message)
        }
        response.data ?: throw NetworkBusinessException(response.code, emptyDataMessage)
    }
}
