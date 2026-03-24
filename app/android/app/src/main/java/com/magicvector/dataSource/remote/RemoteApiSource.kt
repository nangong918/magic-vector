package com.magicvector.dataSource.remote

import com.magicvector.utils.network.BaseResponse
import com.magicvector.domain.constant.BaseConstant
import com.magicvector.MainApplication
import com.magicvector.domain.convertor.AgentChatConvertor
import com.magicvector.domain.convertor.ChatMessageConvertor
import com.magicvector.domain.dto.http.request.AgentDeleteRequest
import com.magicvector.domain.dto.http.request.ControlCommandRequest
import com.magicvector.domain.dto.http.request.UserLoginRequest
import com.magicvector.domain.dto.http.request.UserPasswordUpdateRequest
import com.magicvector.domain.dto.http.request.UserTokenVerifyRequest
import com.magicvector.domain.dto.http.request.VideoUploadCompleteRequest
import com.magicvector.domain.dto.http.request.VideoUploadInitRequest
import com.magicvector.domain.dto.http.response.AgentResponse
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
import com.magicvector.domain.exception.NetworkBusinessException
import com.magicvector.domain.exception.NetworkParamIllegalException
import com.magicvector.domain.model.agent.AgentChatModel
import com.magicvector.domain.model.chat.ChatMessageModel
import com.magicvector.repository.api.ApiRequest
import com.magicvector.utils.auth.AuthTokenHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody

class RemoteApiSource(
    private val apiRequest: ApiRequest
) {

    private suspend fun <T> requestData(
        apiCall: suspend () -> BaseResponse<T>,
        emptyDataMessage: String = "响应数据为空"
    ): T = withContext(Dispatchers.IO) {
        val response = apiCall()
        if (BaseConstant.NetworkCode.SUCCESS_CODE != response.code) {
            if (AuthTokenHandler.isTokenExpiredCode(response.code?: "")) {
                AuthTokenHandler.handleTokenExpired(MainApplication.getApp())
            }
            throw NetworkBusinessException(response.code, response.message)
        }
        response.data ?: throw NetworkBusinessException(response.code, emptyDataMessage)
    }

    suspend fun verifyAccessToken(
        accessToken: String
    ): UserTokenVerifyResponse {
        val localUser = MainApplication.getUserManager().getCurrentUser()
        if (localUser == null || localUser.userId <= 0L || accessToken.isBlank()) {
            throw NetworkParamIllegalException("用户不存在")
        }

        val request = UserTokenVerifyRequest().apply {
            userId = localUser.userId
            this.accessToken = accessToken
        }

        return requestData(apiCall = { apiRequest.verifyAccessToken(request) }, emptyDataMessage = "Token验证响应为空")
    }

    suspend fun createAgent(
        avatar: MultipartBody.Part?,
        userId: RequestBody,
        name: RequestBody,
        description: RequestBody
    ): AgentChatModel {
        val response = requestData(
            apiCall = { apiRequest.createAgent(avatar, userId, name, description) },
            emptyDataMessage = "创建Agent响应为空"
        )
        return AgentChatConvertor.dto2Model(response.agent)
    }

    suspend fun getAgentInfo(agentId: String): AgentChatModel {
        val response = requestData(
            apiCall = { apiRequest.getAgentInfo(agentId) },
            emptyDataMessage = "Agent详情响应为空"
        )
        return AgentChatConvertor.dto2Model(response.agent)
    }

    /**
     * 获取Agent列表
     * @param userId 用户Id
     * @return Agent列表
     */
    suspend fun getAgentListFull(userId: String): List<AgentChatModel>{
        val response = requestData(
            apiCall = { apiRequest.getAgentListFull(userId) },
            emptyDataMessage = "Agent列表响应为空"
        )
        return AgentChatConvertor.dtos2Models(response.agentList)
    }

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
    suspend fun getAgentListPage(
        userId: String,
        sortField: String,
        sortOrder: String,
        pageDirection: String,
        cursor: String,
        limit: Int
    ): List<AgentChatModel> {
        val response = requestData(
            apiCall = { apiRequest.getAgentListPage(userId, sortField, sortOrder, pageDirection, cursor, limit) },
            emptyDataMessage = "Agent分页列表响应为空"
        )
        return AgentChatConvertor.dtos2Models(response.agentList)
    }

    suspend fun updateAgent(
        avatar: MultipartBody.Part?,
        agentId: RequestBody,
        userId: RequestBody,
        name: RequestBody,
        description: RequestBody
    ): AgentChatModel {
        val response = requestData(
            apiCall = { apiRequest.updateAgent(avatar, agentId, userId, name, description) },
            emptyDataMessage = "更新Agent响应为空"
        )
        return AgentChatConvertor.dto2Model(response.agent)
    }

    suspend fun deleteAgent(request: AgentDeleteRequest): AgentResponse {
        return requestData(
            apiCall = { apiRequest.deleteAgent(request) },
            emptyDataMessage = "删除Agent响应为空"
        )
    }


    /**
     * 全量获取聊天消息列表
     * @param agentId AgentId
     * @param userId 用户Id
     * @return 聊天消息列表
     */
    suspend fun getChatListFull(
        agentId: String,
        userId: String
    ): List<ChatMessageModel> {
        val response = requestData(
            apiCall = { apiRequest.getChatListFull(agentId, userId) },
            emptyDataMessage = "聊天消息列表响应为空"
        )
        return ChatMessageConvertor.dtos2Models(response.messageList)
    }

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
    suspend fun getChatListPage(
        agentId: String,
        userId: String,
        sortField: String,
        sortOrder: String,
        pageDirection: String,
        cursor: String,
        limit: Int
    ): List<ChatMessageModel> {
        val response = requestData(
            apiCall = { apiRequest.getChatListPage(agentId, userId, sortField, sortOrder, pageDirection, cursor, limit) },
            emptyDataMessage = "聊天消息分页列表响应为空"
        )
        return ChatMessageConvertor.dtos2Models(response.messageList)
    }

    suspend fun uploadImageVision(
        images: List<MultipartBody.Part>,
        agentId: RequestBody,
        userId: RequestBody,
        messageId: RequestBody
    ): String {
        return requestData(
            apiCall = { apiRequest.uploadImageVision(images, agentId, userId, messageId) },
            emptyDataMessage = "视觉上传响应为空"
        )
    }

    suspend fun getControlStatus(deviceId: String): ControlStatusResponse {
        return requestData(
            apiCall = { apiRequest.getControlStatus(deviceId) },
            emptyDataMessage = "控制状态响应为空"
        )
    }

    suspend fun sendControlCommand(request: ControlCommandRequest): ControlCommandResponse {
        return requestData(
            apiCall = { apiRequest.sendControlCommand(request) },
            emptyDataMessage = "控制命令响应为空"
        )
    }

    suspend fun getControlAgentLogs(
        userId: String,
        agentId: String?,
        page: Int,
        size: Int
    ): ControlAgentLogResponse {
        return requestData(
            apiCall = { apiRequest.getControlAgentLogs(userId, agentId, page, size) },
            emptyDataMessage = "控制日志响应为空"
        )
    }

    suspend fun initVideoUpload(request: VideoUploadInitRequest): VideoUploadInitResponse {
        return requestData(
            apiCall = { apiRequest.initVideoUpload(request) },
            emptyDataMessage = "初始化上传响应为空"
        )
    }

    suspend fun uploadVideoChunk(
        uploadId: RequestBody,
        userId: RequestBody,
        chunkIndex: RequestBody,
        offset: RequestBody,
        chunkFile: MultipartBody.Part
    ): VideoUploadChunkResponse {
        return requestData(
            apiCall = { apiRequest.uploadVideoChunk(uploadId, userId, chunkIndex, offset, chunkFile) },
            emptyDataMessage = "上传分片响应为空"
        )
    }

    suspend fun completeVideoUpload(request: VideoUploadCompleteRequest): VideoUploadCompleteResponse {
        return requestData(
            apiCall = { apiRequest.completeVideoUpload(request) },
            emptyDataMessage = "完成上传响应为空"
        )
    }

    suspend fun getCloudVideoList(
        userId: String,
        page: Int,
        size: Int
    ): VideoCloudListResponse {
        return requestData(
            apiCall = { apiRequest.getCloudVideoList(userId, page, size) },
            emptyDataMessage = "云视频列表响应为空"
        )
    }

    suspend fun getCloudVideoPlayUrl(videoId: String): VideoPlayUrlResponse {
        return requestData(
            apiCall = { apiRequest.getCloudVideoPlayUrl(videoId) },
            emptyDataMessage = "云视频播放地址响应为空"
        )
    }

    suspend fun getCloudVideoDownloadUrl(videoId: String): VideoDownloadUrlResponse {
        return requestData(
            apiCall = { apiRequest.getCloudVideoDownloadUrl(videoId) },
            emptyDataMessage = "云视频下载地址响应为空"
        )
    }

    suspend fun register(
        avatar: MultipartBody.Part?,
        account: RequestBody,
        password: RequestBody,
        name: RequestBody
    ): UserAuthResponse {
        return requestData(
            apiCall = {
                apiRequest.register(
                    avatar = avatar,
                    account = account,
                    password = password,
                    name = name
                )
            },
            emptyDataMessage = "注册响应为空"
        )
    }

    suspend fun login(request: UserLoginRequest): UserAuthResponse {
        return requestData(
            apiCall = { apiRequest.login(request) },
            emptyDataMessage = "登录响应为空"
        )
    }

    suspend fun updatePassword(request: UserPasswordUpdateRequest): UserPasswordUpdateResponse {
        return requestData(
            apiCall = { apiRequest.updatePassword(request) },
            emptyDataMessage = "修改密码响应为空"
        )
    }
}
