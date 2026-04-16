package com.vectordemo.dataSource.remote

import com.vectordemo.MainApplication
import com.vectordemo.domain.convertor.UserConvertor
import com.vectordemo.domain.dto.http.request.UserLoginRequest
import com.vectordemo.domain.dto.http.request.UserPasswordUpdateRequest
import com.vectordemo.domain.dto.http.request.UserTokenVerifyRequest
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.domain.model.user.UserSessionModel
import com.vectordemo.repository.api.ApiRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 用户域远程数据源：不对外暴露 DTO/Response，仅返回业务 [UserSessionModel] 等。
 */
class UserRemoteApiSource(private val apiRequest: ApiRequest) {

    suspend fun login(account: String, password: String): UserSessionModel {
        val auth = RemoteRequestData.requestData(
            { apiRequest.login(UserLoginRequest(account = account, password = password)) },
            "登录响应为空",
        )
        return UserConvertor.authResponseToSessionModel(auth, password)
    }

    suspend fun register(
        avatar: MultipartBody.Part?,
        account: String,
        password: String,
        name: String,
    ): UserSessionModel {
        val auth = RemoteRequestData.requestData(
            {
                apiRequest.register(
                    avatar,
                    account.toRequestBody("text/plain".toMediaTypeOrNull()),
                    password.toRequestBody("text/plain".toMediaTypeOrNull()),
                    name.toRequestBody("text/plain".toMediaTypeOrNull()),
                )
            },
            "注册响应为空",
        )
        return UserConvertor.authResponseToSessionModel(auth, password = password)
    }

    suspend fun verifyAccessToken(accessToken: String): Boolean {
        val localUser = MainApplication.getUserManager().getCurrentUser()
        if (localUser == null || localUser.userId <= 0L || accessToken.isBlank()) {
            throw NetworkParamIllegalException("用户不存在")
        }
        val verify = RemoteRequestData.requestData(
            {
                apiRequest.verifyAccessToken(
                    UserTokenVerifyRequest(userId = localUser.userId.toString(), accessToken = accessToken),
                )
            },
            "Token验证响应为空",
        )
        return verify.valid == true
    }

    suspend fun updatePassword(userId: Long, oldPassword: String, newPassword: String) {
        RemoteRequestData.requestData(
            {
                apiRequest.updatePassword(
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
