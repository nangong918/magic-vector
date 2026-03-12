package com.data.dao.api

import com.core.appcore.api.ApiRequest
import com.core.baseutil.network.BaseApiRequestImpl
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.dto.request.AgentDeleteRequest
import com.data.domain.dto.request.ChatByAnchorRequest
import com.data.domain.dto.request.ControlCommandRequest
import com.data.domain.dto.request.UserLoginRequest
import com.data.domain.dto.request.UserTokenVerifyRequest
import com.data.domain.dto.response.AgentLastChatListResponse
import com.data.domain.dto.response.AgentListResponse
import com.data.domain.dto.response.AgentResponse
import com.data.domain.dto.response.ChatMessageResponse
import com.data.domain.dto.response.ControlCommandResponse
import com.data.domain.dto.response.ControlStatusResponse
import com.data.domain.dto.response.UserAuthResponse
import com.data.domain.dto.response.UserTokenVerifyResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody

open class ApiRequestImpl(apiRequest: ApiRequest) : BaseApiRequestImpl() {

    // mApi 可以直接使用构造函数参数
    private val mApi: ApiRequest = apiRequest

    //    @Multipart
    //    @POST("/agent/create")
    //    suspend fun createAgent(
    //        @Part avatar: MultipartBody.Part,
    //        @Part("userId") userId: RequestBody,
    //        @Part("name") name: RequestBody,
    //        @Part("description") description: RequestBody
    //    ): BaseResponse<AgentResponse>
    fun createAgent(
        avatar: MultipartBody.Part?,
        userId: RequestBody,
        name: RequestBody,
        description: RequestBody,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = {
                mApi.createAgent(
                    avatar,
                    userId,
                    name,
                    description
                )
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    //    @GET("/agent/getInfo")
    //    suspend fun getAgentInfo(
    //        @Query("agentId") agentId: String
    //    ): BaseResponse<AgentResponse>
    fun getAgentInfo(
        agentId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentResponse>>?,
        throwableCallback: OnThrowableCallback?
    ){
        sendRequestCallback(
            apiCall = {
                mApi.getAgentInfo(agentId)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    //    @GET("/agent/getList")
    //    suspend fun getAgentList(
    //        @Query("userId") userId: String
    //    ): BaseResponse<AgentListResponse>
    fun getAgentList(
        userId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentListResponse>>?,
        throwableCallback: OnThrowableCallback?
    ){
        sendRequestCallback(
            apiCall = {
                mApi.getAgentList(userId)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun updateAgent(
        avatar: MultipartBody.Part?,
        agentId: RequestBody,
        userId: RequestBody,
        name: RequestBody,
        description: RequestBody,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = {
                mApi.updateAgent(avatar, agentId, userId, name, description)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun deleteAgent(
        request: AgentDeleteRequest,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { mApi.deleteAgent(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    //    @GET("/agent/getLastAgentChatList")
    //    suspend fun getLastAgentChatList(
    //        @Query("userId") userId: String
    //    ): BaseResponse<AgentLastChatListResponse>
    fun getLastAgentChatList(
        userId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentLastChatListResponse>>?,
        throwableCallback: OnThrowableCallback?
    ){
        sendRequestCallback(
            apiCall = {
                mApi.getLastAgentChatList(userId)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    //    @GET("/chat/getLastChat")
    //    suspend fun getLastChat(
    //        @Query("agentId") agentId: String
    //    ): BaseResponse<ChatMessageResponse>
    fun getLastChat(
        agentId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ChatMessageResponse>>?,
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
    fun getTimeLimitChat(
        agentId: String,
        // yyyy-MM-dd HH:mm:ss
        deadline: String,
        // max 50
        limit: Int,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ChatMessageResponse>>?,
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

    fun getChatByAnchor(
        agentId: String,
        anchorTimestamp: Long,
        before: Boolean,
        limit: Int,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ChatMessageResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        val request = ChatByAnchorRequest().apply {
            this.agentId = agentId
            this.anchorTimestamp = anchorTimestamp
            this.before = before
            this.limit = limit
        }
        sendRequestCallback(
            apiCall = {
                mApi.getChatByAnchor(request)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    //    @Multipart
    //    @POST("/chat/vision/upload/img")
    //    suspend fun uploadImageVision(
    //        @Part images: List<MultipartBody.Part>,
    //        @Part("agentId") agentId: RequestBody,
    //        @Part("userId") userId: RequestBody,
    //        @Part("messageId") messageId: RequestBody,
    //    ): BaseResponse<String>
    fun uploadImageVision(
        images: List<MultipartBody.Part>,
        agentId: RequestBody,
        userId: RequestBody,
        messageId: RequestBody,
        onSuccessCallback: OnSuccessCallback<BaseResponse<String>>?,
        throwableCallback: OnThrowableCallback?
    ){
        sendRequestCallback(
            apiCall = {
                mApi.uploadImageVision(
                    images,
                    agentId,
                    userId,
                    messageId
                )
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getControlStatus(
        deviceId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ControlStatusResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { mApi.getControlStatus(deviceId) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun sendControlCommand(
        request: ControlCommandRequest,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ControlCommandResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { mApi.sendControlCommand(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun register(
        avatar: MultipartBody.Part?,
        account: RequestBody,
        password: RequestBody,
        name: RequestBody,
        onSuccessCallback: OnSuccessCallback<BaseResponse<UserAuthResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = {
                mApi.register(
                    avatar = avatar,
                    account = account,
                    password = password,
                    name = name
                )
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun login(
        request: UserLoginRequest,
        onSuccessCallback: OnSuccessCallback<BaseResponse<UserAuthResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = {
                mApi.login(request)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun verifyAccessToken(
        request: UserTokenVerifyRequest,
        onSuccessCallback: OnSuccessCallback<BaseResponse<UserTokenVerifyResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = {
                mApi.verifyAccessToken(request)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }
}