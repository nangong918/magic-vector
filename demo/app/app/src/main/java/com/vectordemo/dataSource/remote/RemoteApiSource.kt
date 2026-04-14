package com.vectordemo.dataSource.remote

import com.vectordemo.MainApplication
import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.domain.dto.http.request.UserLoginRequest
import com.vectordemo.domain.dto.http.request.UserPasswordUpdateRequest
import com.vectordemo.domain.dto.http.request.UserTokenVerifyRequest
import com.vectordemo.domain.dto.http.response.OssUserBucketFileIdsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileUrlsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketListResponse
import com.vectordemo.domain.dto.http.response.UserAuthResponse
import com.vectordemo.domain.dto.http.response.UserPasswordUpdateResponse
import com.vectordemo.domain.dto.http.response.UserTokenVerifyResponse
import com.vectordemo.domain.exception.NetworkBusinessException
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.repository.api.ApiRequest
import com.vectordemo.utils.auth.AuthTokenHandler
import com.vectordemo.utils.network.BaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody

class RemoteApiSource(private val apiRequest: ApiRequest) {
    private suspend fun <T> requestData(apiCall: suspend () -> BaseResponse<T>, emptyDataMessage: String = "响应数据为空"): T =
        withContext(Dispatchers.IO) {
            val response = apiCall()
            if (BaseConstant.NetworkCode.SUCCESS_CODE != response.code) {
                if (AuthTokenHandler.isTokenExpiredCode(response.code ?: "")) {
                    AuthTokenHandler.handleTokenExpired(MainApplication.getApp())
                }
                throw NetworkBusinessException(response.code, response.message)
            }
            response.data ?: throw NetworkBusinessException(response.code, emptyDataMessage)
        }

    suspend fun verifyAccessToken(accessToken: String): UserTokenVerifyResponse {
        val localUser = MainApplication.getUserManager().getCurrentUser()
        if (localUser == null || localUser.userId <= 0L || accessToken.isBlank()) {
            throw NetworkParamIllegalException("用户不存在")
        }
        return requestData({
            apiRequest.verifyAccessToken(
                UserTokenVerifyRequest(userId = localUser.userId.toString(), accessToken = accessToken)
            )
        }, "Token验证响应为空")
    }

    suspend fun register(avatar: MultipartBody.Part?, account: RequestBody, password: RequestBody, name: RequestBody): UserAuthResponse {
        return requestData({ apiRequest.register(avatar, account, password, name) }, "注册响应为空")
    }

    suspend fun login(request: UserLoginRequest): UserAuthResponse {
        return requestData({ apiRequest.login(request) }, "登录响应为空")
    }

    suspend fun updatePassword(userId: Long, oldPassword: String, newPassword: String): UserPasswordUpdateResponse {
        return requestData(
            {
                apiRequest.updatePassword(
                    UserPasswordUpdateRequest(
                        userId = userId.toString(),
                        oldPassword = oldPassword,
                        newPassword = newPassword
                    )
                )
            },
            "修改密码响应为空"
        )
    }

    suspend fun ossUserBucketList(userId: String): OssUserBucketListResponse {
        return requestData({ apiRequest.ossUserBucketList(userId) }, "存储桶列表响应为空")
    }

    suspend fun ossUserBucketFileIdList(userId: String, bucketName: String): OssUserBucketFileIdsResponse {
        return requestData(
            { apiRequest.ossUserBucketFileIdList(userId, bucketName) },
            "文件 id 列表响应为空"
        )
    }

    suspend fun ossUserBucketFileUrlList(userId: String, bucketName: String): OssUserBucketFileUrlsResponse {
        return requestData(
            { apiRequest.ossUserBucketFileUrlList(userId, bucketName) },
            "文件 URL 列表响应为空"
        )
    }
}
