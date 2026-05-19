package com.vectordemo.dataSource.remote

import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.domain.dto.http.response.BaseResponse
import com.vectordemo.domain.exception.NetworkBusinessException

internal object RemoteRequestData {
    suspend fun <T> requestData(
        apiCall: suspend () -> BaseResponse<T>,
        emptyDataMessage: String = "响应数据为空",
    ): T {
        val response = apiCall()
        if (BaseConstant.NetworkCode.SUCCESS_CODE != response.code) {
            throw NetworkBusinessException(response.code, response.message)
        }
        return response.data ?: throw NetworkBusinessException(response.code, emptyDataMessage)
    }
}
