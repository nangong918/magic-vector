package com.magicvector.repository.api

import com.magicvector.utils.network.BaseResponse
import com.magicvector.domain.dto.http.request.AgentDeleteRequest
import com.magicvector.domain.dto.http.request.ControlCommandRequest
import com.magicvector.domain.dto.http.request.UserLoginRequest
import com.magicvector.domain.dto.http.request.UserPasswordUpdateRequest
import com.magicvector.domain.dto.http.request.UserTokenVerifyRequest
import com.magicvector.domain.dto.http.request.VideoUploadCompleteRequest
import com.magicvector.domain.dto.http.request.VideoUploadInitRequest
import com.magicvector.domain.dto.http.response.AgentListResponse
import com.magicvector.domain.dto.http.response.AgentResponse
import com.magicvector.domain.dto.http.response.ChatMessageListResponse
import com.magicvector.domain.dto.http.response.ControlAgentLogResponse
import com.magicvector.domain.dto.http.response.ControlCommandResponse
import com.magicvector.domain.dto.http.response.ControlStatusResponse
import com.magicvector.domain.dto.http.response.UserAuthResponse
import com.magicvector.domain.dto.http.response.UserPasswordUpdateResponse
import com.magicvector.domain.dto.http.response.UserTokenVerifyResponse
import com.magicvector.domain.dto.http.response.VideoCloudListResponse
import com.magicvector.domain.dto.http.response.VideoDownloadUrlResponse
import com.magicvector.domain.dto.http.response.VideoPlayUrlResponse
import com.magicvector.domain.dto.http.response.VideoUploadChunkResponse
import com.magicvector.domain.dto.http.response.VideoUploadCompleteResponse
import com.magicvector.domain.dto.http.response.VideoUploadInitResponse
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
     * 全量获取Agent列表
     * @param userId   用户Id
     * @return  Agent列表
     */
    @GET("/agent/getListFull")
    suspend fun getAgentListFull(
        @Query("userId") userId: String
    ): BaseResponse<AgentListResponse>

    /**
     * 分页获取Agent列表
     * @param userId 用户Id
     * @param sortField 排序字段 (lastChatTime, agentId, name)
     * @param sortOrder 排序顺序 (ASC, DESC)
     * @param pageDirection 分页方向 (after, before)
     * @param cursor 游标值
     * @param limit 查询条数
     * @return Agent列表
     */
    @GET("/agent/getListPage")
    suspend fun getAgentListPage(
        @Query("userId") userId: String,
        @Query("sortField") sortField: String,
        @Query("sortOrder") sortOrder: String,
        @Query("pageDirection") pageDirection: String,
        @Query("cursor") cursor: String,
        @Query("limit") limit: Int
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


    //==========Chat

    /**
     * 全量获取聊天消息列表
     * @param agentId AgentId
     * @param userId 用户Id
     * @return 聊天消息列表
     */
    @GET("/chat/getListFull")
    suspend fun getChatListFull(
        @Query("agentId") agentId: String,
        @Query("userId") userId: String
    ): BaseResponse<ChatMessageListResponse>

    /**
     * 分页获取聊天消息列表
     * @param agentId AgentId
     * @param userId 用户Id
     * @param sortField 排序字段 (timestamp, messageId)
     * @param sortOrder 排序顺序 (ASC, DESC)
     * @param pageDirection 分页方向 (after, before)
     * @param cursor 游标值
     * @param limit 查询条数
     * @return 聊天消息列表
     */
    @GET("/chat/getListPage")
    suspend fun getChatListPage(
        @Query("agentId") agentId: String,
        @Query("userId") userId: String,
        @Query("sortField") sortField: String,
        @Query("sortOrder") sortOrder: String,
        @Query("pageDirection") pageDirection: String,
        @Query("cursor") cursor: String,
        @Query("limit") limit: Int
    ): BaseResponse<ChatMessageListResponse>

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

    @GET("/control/log/list")
    suspend fun getControlAgentLogs(
        @Query("userId") userId: String,
        @Query("agentId") agentId: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): BaseResponse<ControlAgentLogResponse>

    //==========Video

    @POST("/video/upload/init")
    suspend fun initVideoUpload(
        @Body request: VideoUploadInitRequest
    ): BaseResponse<VideoUploadInitResponse>

    @Multipart
    @POST("/video/upload/chunk")
    suspend fun uploadVideoChunk(
        @Part("uploadId") uploadId: RequestBody,
        @Part("userId") userId: RequestBody,
        @Part("chunkIndex") chunkIndex: RequestBody,
        @Part("offset") offset: RequestBody,
        @Part chunkFile: MultipartBody.Part
    ): BaseResponse<VideoUploadChunkResponse>

    @POST("/video/upload/complete")
    suspend fun completeVideoUpload(
        @Body request: VideoUploadCompleteRequest
    ): BaseResponse<VideoUploadCompleteResponse>

    @GET("/video/cloud/list")
    suspend fun getCloudVideoList(
        @Query("userId") userId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): BaseResponse<VideoCloudListResponse>

    @GET("/video/cloud/play-url")
    suspend fun getCloudVideoPlayUrl(
        @Query("videoId") videoId: String
    ): BaseResponse<VideoPlayUrlResponse>

    @GET("/video/cloud/download-url")
    suspend fun getCloudVideoDownloadUrl(
        @Query("videoId") videoId: String
    ): BaseResponse<VideoDownloadUrlResponse>

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

    @POST("/user/password/update")
    suspend fun updatePassword(
        @Body request: UserPasswordUpdateRequest
    ): BaseResponse<UserPasswordUpdateResponse>
}