package com.core.appcore.api

import com.core.baseutil.network.BaseResponse
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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiRequest {

    //==========Agent

    /**
     * 创建Agent
     * @param avatar        头像
     * @param name          昵称
     * @param description   提示词设定
     * @return  创建结果
     */
    @Multipart
    @POST("/agent/create")
    suspend fun createAgent(
        @Part avatar: MultipartBody.Part?,
        @Part("userId") userId: RequestBody,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody
    ): BaseResponse<AgentResponse>

    /**
     * 获取Agent信息
     * @param agentId   AgentId
     * @return  Agent信息
     */
    @GET("/agent/getInfo")
    suspend fun getAgentInfo(
        @Query("agentId") agentId: String
    ): BaseResponse<AgentResponse>

    /**
     * 获取Agent列表
     * @param userId   用户Id
     * @return  Agent列表
     */
    @GET("/agent/getList")
    suspend fun getAgentList(
        @Query("userId") userId: String
    ): BaseResponse<AgentListResponse>

    @Multipart
    @POST("/agent/update")
    suspend fun updateAgent(
        @Part avatar: MultipartBody.Part?,
        @Part("agentId") agentId: RequestBody,
        @Part("userId") userId: RequestBody,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody
    ): BaseResponse<AgentResponse>

    @POST("/agent/delete")
    suspend fun deleteAgent(
        @Body request: AgentDeleteRequest
    ): BaseResponse<AgentResponse>

    /**
     * 获取LastAgentChat列表
     * @param userId   用户Id
     * @return  LastAgentChat列表
     */
    @GET("/agent/getLastAgentChatList")
    suspend fun getLastAgentChatList(
        @Query("userId") userId: String
    ): BaseResponse<AgentLastChatListResponse>

    //==========Chat

    /**
     * 获取最新的20条消息
     * @param agentId   agentId
     * @return  最新的20条消息
     */
    @GET("/chat/getLastChat")
    suspend fun getLastChat(
        @Query("agentId") agentId: String
    ): BaseResponse<ChatMessageResponse>

    /**
     * 获取指定时间段的消息
     * @param agentId    agentId
     * @param deadline   最后时间; yyyy-MM-dd HH:mm:ss
     * @param limit      限制：max 50
     * @return  指定时间段消息
     */
    @GET("/chat/getTimeLimitChat")
    suspend fun getTimeLimitChat(
        @Query("agentId") agentId: String,
        // yyyy-MM-dd HH:mm:ss
        @Query("deadline") deadline: String,
        // max 50
        @Query("limit") limit: Int,
    ): BaseResponse<ChatMessageResponse>

    @POST("/chat/getByAnchor")
    suspend fun getChatByAnchor(
        @Body request: ChatByAnchorRequest
    ): BaseResponse<ChatMessageResponse>

    /**
     * http上传image的vision任务
     * @param image         image
     * @param agentId       agentId
     * @param userId        userId
     * @param messageId     messageId
     */
    @Multipart
    @POST("/chat/vision/upload/img")
    suspend fun uploadImageVision(
        @Part images: List<MultipartBody.Part>,
        @Part("agentId") agentId: RequestBody,
        @Part("userId") userId: RequestBody,
        @Part("messageId") messageId: RequestBody,
    ): BaseResponse<String>

    //==========Control

    @GET("/control/status")
    suspend fun getControlStatus(
        @Query("deviceId") deviceId: String
    ): BaseResponse<ControlStatusResponse>

    @POST("/control/command")
    suspend fun sendControlCommand(
        @Body request: ControlCommandRequest
    ): BaseResponse<ControlCommandResponse>

    //==========User

    // register 与后端约定为 Multipart/FormData（不能改成 JSON Body）
    @Multipart
    @POST("/user/register")
    suspend fun register(
        @Part avatar: MultipartBody.Part?,
        @Part("account") account: RequestBody,
        @Part("password") password: RequestBody,
        @Part("name") name: RequestBody
    ): BaseResponse<UserAuthResponse>

    @POST("/user/login")
    suspend fun login(
        @Body request: UserLoginRequest
    ): BaseResponse<UserAuthResponse>

    @POST("/user/token/verify")
    suspend fun verifyAccessToken(
        @Body request: UserTokenVerifyRequest
    ): BaseResponse<UserTokenVerifyResponse>
}