package com.vectordemo.dataSource.remote

import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.domain.model.user.UserSessionModel
import com.vectordemo.repository.api.ApiRequest

class UserRemoteApiSource(private val apiRequest: ApiRequest) {
    suspend fun login(account: String, password: String): UserSessionModel {
        val auth = RemoteRequestData.requestData(
            { apiRequest.login(UserSessionModel.loginRequest(account, password)) },
            "登录响应为空",
        )
        return UserSessionModel.fromAuthResponse(auth, password)
    }

    suspend fun register(
        avatar: MultipartPartPayload?,
        account: String,
        password: String,
        name: String,
    ): UserSessionModel {
        val auth = RemoteRequestData.requestData(
            { apiRequest.register(avatar, account, password, name) },
            "注册响应为空",
        )
        return UserSessionModel.fromAuthResponse(auth, password)
    }

    suspend fun verifyAccessToken(userId: Long, accessToken: String): Boolean {
        if (userId <= 0L || accessToken.isBlank()) throw NetworkParamIllegalException("用户不存在")
        val verify = RemoteRequestData.requestData(
            { apiRequest.verifyAccessToken(UserSessionModel.TokenVerify.request(userId, accessToken)) },
            "Token验证响应为空",
        )
        return UserSessionModel.TokenVerify.parseValid(verify)
    }

    suspend fun updatePassword(userId: Long, oldPassword: String, newPassword: String) {
        RemoteRequestData.requestData(
            {
                apiRequest.updatePassword(
                    UserSessionModel.PasswordUpdate.request(userId, oldPassword, newPassword),
                )
            },
            "修改密码响应为空",
        )
    }
}
