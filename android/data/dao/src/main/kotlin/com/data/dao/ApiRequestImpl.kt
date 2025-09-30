package com.data.dao

import com.core.appcore.api.ApiRequest
import com.core.baseutil.network.BaseApiRequestImpl
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.dto.response.ChatMessageResponse

open class ApiRequestImpl(apiRequest: ApiRequest) : BaseApiRequestImpl() {

    // mApi 可以直接使用构造函数参数
    private val mApi: ApiRequest = apiRequest

    //    @GET("/chat/getLastChat")
    //    suspend fun getLastChat(
    //        @Query("agentId") agentId: String
    //    ): BaseResponse<ChatMessageResponse>
    fun <T> getLastChat(
        agentId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ChatMessageResponse>>? ,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { mApi.getLastChat(agentId) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    //    @GET("/chat/getTimeLimitChat")
    //    suspend fun getTimeLimitChat(
    //        @Query("agentId") agentId: String,
    //        // yyyy-MM-dd HH:mm:ss
    //        @Query("deadline") deadline: String,
    //        // max 50
    //        @Query("limit") limit: Int,
    //    ): BaseResponse<ChatMessageResponse>
    fun <T> getTimeLimitChat(
        agentId: String,
        // yyyy-MM-dd HH:mm:ss
        deadline: String,
        // max 50
        limit: Int,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ChatMessageResponse>>? ,
        throwableCallback: OnThrowableCallback?
    ){
        sendRequestCallback(
            apiCall = {
                mApi.getTimeLimitChat(
                    agentId = agentId,
                    deadline = deadline,
                    limit = limit
                )
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }
}