package com.magicvector.dataSource.remote

import com.core.baseutil.network.BaseResponse
import com.data.domain.constant.BaseConstant
import com.magicvector.MainApplication
import com.magicvector.domain.dto.http.request.AgentDeleteRequest
import com.magicvector.domain.dto.http.request.ChatByAnchorRequest
import com.magicvector.domain.dto.http.request.ControlCommandRequest
import com.magicvector.domain.dto.http.request.UserLoginRequest
import com.magicvector.domain.dto.http.request.UserPasswordUpdateRequest
import com.magicvector.domain.dto.http.request.UserTokenVerifyRequest
import com.magicvector.domain.dto.http.request.VideoUploadCompleteRequest
import com.magicvector.domain.dto.http.request.VideoUploadInitRequest
import com.magicvector.domain.dto.http.response.AgentLastChatListResponse
import com.magicvector.domain.dto.http.response.AgentListResponse
import com.magicvector.domain.dto.http.response.AgentResponse
import com.magicvector.domain.dto.http.response.ChatMessageResponse
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
    ): AgentResponse {
        return requestData(
            apiCall = { apiRequest.createAgent(avatar, userId, name, description) },
            emptyDataMessage = "创建Agent响应为空"
        )
    }

    suspend fun getAgentInfo(agentId: String): AgentResponse {
        return requestData(
            apiCall = { apiRequest.getAgentInfo(agentId) },
            emptyDataMessage = "Agent详情响应为空"
        )
    }

    suspend fun getAgentList(userId: String): AgentListResponse {
        return requestData(
            apiCall = { apiRequest.getAgentList(userId) },
            emptyDataMessage = "Agent列表响应为空"
        )
    }

    suspend fun updateAgent(
        avatar: MultipartBody.Part?,
        agentId: RequestBody,
        userId: RequestBody,
        name: RequestBody,
        description: RequestBody
    ): AgentResponse {
        return requestData(
            apiCall = { apiRequest.updateAgent(avatar, agentId, userId, name, description) },
            emptyDataMessage = "更新Agent响应为空"
        )
    }

    suspend fun deleteAgent(request: AgentDeleteRequest): AgentResponse {
        return requestData(
            apiCall = { apiRequest.deleteAgent(request) },
            emptyDataMessage = "删除Agent响应为空"
        )
    }

    suspend fun getLastAgentChatList(userId: String): AgentLastChatListResponse {
        return requestData(
            apiCall = { apiRequest.getLastAgentChatList(userId) },
            emptyDataMessage = "最近会话列表响应为空"
        )
    }

    suspend fun getLastChat(agentId: String): ChatMessageResponse {
        return requestData(
            apiCall = { apiRequest.getLastChat(agentId) },
            emptyDataMessage = "聊天记录响应为空"
        )
    }

    suspend fun getTimeLimitChat(
        agentId: String,
        deadline: String,
        limit: Int
    ): ChatMessageResponse {
        return requestData(
            apiCall = { apiRequest.getTimeLimitChat(agentId = agentId, deadline = deadline, limit = limit) },
            emptyDataMessage = "时间段聊天记录响应为空"
        )
    }

    suspend fun getChatByAnchor(
        agentId: String,
        anchorTimestamp: Long,
        before: Boolean,
        limit: Int
    ): ChatMessageResponse {
        val request = ChatByAnchorRequest().apply {
            this.agentId = agentId
            this.anchorTimestamp = anchorTimestamp
            this.before = before
            this.limit = limit
        }
        return requestData(
            apiCall = { apiRequest.getChatByAnchor(request) },
            emptyDataMessage = "锚点聊天记录响应为空"
        )
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
