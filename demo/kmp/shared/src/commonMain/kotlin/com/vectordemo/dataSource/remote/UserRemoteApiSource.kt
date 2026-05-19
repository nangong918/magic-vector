package com.vectordemo.dataSource.remote

import com.vectordemo.domain.convertor.UserConvertor
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.domain.model.user.UserSessionModel
import com.vectordemo.repository.api.ApiClient
import com.vectordemo.repository.api.MultipartPartPayload
import com.vectordemo.repository.api.UserLoginRequest
import com.vectordemo.repository.api.UserPasswordUpdateRequest
import com.vectordemo.repository.api.UserTokenVerifyRequest

class UserRemoteApiSource(private val apiClient: ApiClient) {
    suspend fun login(account: String, password: String): UserSessionModel {
        val auth = RemoteRequestData.requestData(
            { apiClient.login(UserLoginRequest(account = account, password = password)) },
            "登录响应为空",
        )
        return UserConvertor.authResponseToSessionModel(auth, password)
    }

    suspend fun register(
        avatar: MultipartPartPayload?,
        account: String,
        password: String,
        name: String,
    ): UserSessionModel {
        val auth = RemoteRequestData.requestData(
            { apiClient.register(avatar = avatar, account = account, password = password, name = name) },
            "注册响应为空",
        )
        return UserConvertor.authResponseToSessionModel(auth, password)
    }

    suspend fun verifyAccessToken(userId: Long, accessToken: String): Boolean {
        if (userId <= 0L || accessToken.isBlank()) throw NetworkParamIllegalException("用户不存在")
        val verify = RemoteRequestData.requestData(
            {
                apiClient.verifyAccessToken(
                    UserTokenVerifyRequest(userId = userId.toString(), accessToken = accessToken),
                )
            },
            "Token验证响应为空",
        )
        return verify.valid == true
    }

    suspend fun updatePassword(userId: Long, oldPassword: String, newPassword: String) {
        RemoteRequestData.requestData(
            {
                apiClient.updatePassword(
                    UserPasswordUpdateRequest(
                        userId = userId.toString(),
                        oldPassword = oldPassword,
                        newPassword = newPassword,
                    ),
                )
            },
            "修改密码响应为空",
        )
    }
}
