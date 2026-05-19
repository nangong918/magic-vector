package com.vectordemo.domain.model.user

import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.dto.http.request.UserLoginRequest
import com.vectordemo.domain.dto.http.request.UserPasswordUpdateRequest
import com.vectordemo.domain.dto.http.request.UserTokenVerifyRequest
import com.vectordemo.domain.dto.http.response.UserAuthResponse
import com.vectordemo.domain.dto.http.response.UserPasswordUpdateResponse
import com.vectordemo.domain.dto.http.response.UserTokenVerifyResponse
import com.vectordemo.domain.platform.currentTimeMillis
import com.vectordemo.domain.dto.http.request.appendPayload
import io.ktor.client.request.forms.FormBuilder
import kotlinx.serialization.Serializable

@Serializable
data class UserSessionModel(
    val userId: Long,
    val account: String,
    val name: String,
    val avatarUrl: String = "",
    val accessToken: String,
    val password: String = "",
    val isCurrent: Boolean = false,
    val lastLoginAt: Long = 0L,
) {
    companion object {
        fun loginRequest(account: String, password: String): UserLoginRequest =
            UserLoginRequest(account = account, password = password)

        fun registerFormData(
            avatar: MultipartPartPayload?,
            account: String,
            password: String,
            name: String,
        ): FormBuilder.() -> Unit = {
            if (avatar != null) appendPayload(avatar)
            append("account", account)
            append("password", password)
            append("name", name)
        }

        fun fromAuthResponse(auth: UserAuthResponse, password: String): UserSessionModel {
            val uid = auth.userId?.toLongOrNull() ?: 0L
            return UserSessionModel(
                userId = uid,
                account = auth.account.orEmpty(),
                name = auth.name.orEmpty(),
                avatarUrl = auth.avatarUrl.orEmpty(),
                accessToken = auth.accessToken.orEmpty(),
                password = password,
                isCurrent = true,
                lastLoginAt = currentTimeMillis(),
            )
        }
    }

    object TokenVerify {
        fun request(userId: Long, accessToken: String): UserTokenVerifyRequest =
            UserTokenVerifyRequest(userId = userId.toString(), accessToken = accessToken)

        fun parseValid(response: UserTokenVerifyResponse): Boolean = response.valid == true
    }

    object PasswordUpdate {
        fun request(userId: Long, oldPassword: String, newPassword: String): UserPasswordUpdateRequest =
            UserPasswordUpdateRequest(
                userId = userId.toString(),
                oldPassword = oldPassword,
                newPassword = newPassword,
            )

        fun parse(response: UserPasswordUpdateResponse) {
            // 成功时 data 可能为空；业务码由 RemoteRequestData 校验
        }
    }
}
