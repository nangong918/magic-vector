package com.magicvector.dataSource.remote

import com.core.baseutil.network.BaseApiRequestImpl
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
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
import com.magicvector.repository.api.ApiRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody

class RemoteApiSource(
    private val apiRequest: ApiRequest
) : BaseApiRequestImpl() {

    suspend fun verifyAccessToken(
        accessToken: String,
        handleVerifyAccessToken: (Boolean) -> Unit
    ) {
        val localUser = MainApplication.getUserManager().getCurrentUser()
        if (localUser == null || localUser.userId <= 0L || accessToken.isBlank()) {
            handleVerifyAccessToken(false)
            return
        }
        val request = UserTokenVerifyRequest().apply {
            userId = localUser.userId
            this.accessToken = accessToken
        }
        val response = runCatching { apiRequest.verifyAccessToken(request) }.getOrNull()
        val isSuccessCode = response?.code == BaseConstant.NetworkCode.SUCCESS_CODE
        val isValid = response?.data?.valid == true
        handleVerifyAccessToken(isSuccessCode && isValid)
    }

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
                apiRequest.createAgent(
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

    fun getAgentInfo(
        agentId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getAgentInfo(agentId) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getAgentList(
        userId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentListResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getAgentList(userId) },
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
            apiCall = { apiRequest.updateAgent(avatar, agentId, userId, name, description) },
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
            apiCall = { apiRequest.deleteAgent(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getLastAgentChatList(
        userId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentLastChatListResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getLastAgentChatList(userId) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getLastChat(
        agentId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ChatMessageResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getLastChat(agentId) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getTimeLimitChat(
        agentId: String,
        deadline: String,
        limit: Int,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ChatMessageResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getTimeLimitChat(agentId = agentId, deadline = deadline, limit = limit) },
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
            apiCall = { apiRequest.getChatByAnchor(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun uploadImageVision(
        images: List<MultipartBody.Part>,
        agentId: RequestBody,
        userId: RequestBody,
        messageId: RequestBody,
        onSuccessCallback: OnSuccessCallback<BaseResponse<String>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.uploadImageVision(images, agentId, userId, messageId) },
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
            apiCall = { apiRequest.getControlStatus(deviceId) },
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
            apiCall = { apiRequest.sendControlCommand(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getControlAgentLogs(
        userId: String,
        agentId: String?,
        page: Int,
        size: Int,
        onSuccessCallback: OnSuccessCallback<BaseResponse<ControlAgentLogResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getControlAgentLogs(userId, agentId, page, size) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun initVideoUpload(
        request: VideoUploadInitRequest,
        onSuccessCallback: OnSuccessCallback<BaseResponse<VideoUploadInitResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.initVideoUpload(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun uploadVideoChunk(
        uploadId: RequestBody,
        userId: RequestBody,
        chunkIndex: RequestBody,
        offset: RequestBody,
        chunkFile: MultipartBody.Part,
        onSuccessCallback: OnSuccessCallback<BaseResponse<VideoUploadChunkResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.uploadVideoChunk(uploadId, userId, chunkIndex, offset, chunkFile) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun completeVideoUpload(
        request: VideoUploadCompleteRequest,
        onSuccessCallback: OnSuccessCallback<BaseResponse<VideoUploadCompleteResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.completeVideoUpload(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getCloudVideoList(
        userId: String,
        page: Int,
        size: Int,
        onSuccessCallback: OnSuccessCallback<BaseResponse<VideoCloudListResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getCloudVideoList(userId, page, size) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getCloudVideoPlayUrl(
        videoId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<VideoPlayUrlResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getCloudVideoPlayUrl(videoId) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun getCloudVideoDownloadUrl(
        videoId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<VideoDownloadUrlResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.getCloudVideoDownloadUrl(videoId) },
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
                apiRequest.register(
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
            apiCall = { apiRequest.login(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }

    fun updatePassword(
        request: UserPasswordUpdateRequest,
        onSuccessCallback: OnSuccessCallback<BaseResponse<UserPasswordUpdateResponse>>?,
        throwableCallback: OnThrowableCallback?
    ) {
        sendRequestCallback(
            apiCall = { apiRequest.updatePassword(request) },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }
}
