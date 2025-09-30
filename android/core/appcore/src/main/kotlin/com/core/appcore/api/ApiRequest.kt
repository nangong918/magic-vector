package com.core.appcore.api

import com.core.baseutil.network.BaseResponse
import com.data.domain.dto.response.AgentResponse
import com.data.domain.dto.response.ChatMessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiRequest {

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
        @Part avatar: MultipartBody.Part,
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
}