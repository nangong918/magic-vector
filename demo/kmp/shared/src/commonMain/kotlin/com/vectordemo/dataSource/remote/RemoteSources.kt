package com.vectordemo.dataSource.remote

import com.vectordemo.domain.convertor.OssBatchDeleteModel
import com.vectordemo.domain.convertor.OssBatchUploadModel
import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.convertor.OssFileContentUpdateModel
import com.vectordemo.domain.convertor.UserConvertor
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.domain.model.OssBucketFileItemListModel
import com.vectordemo.domain.model.OssUserBucketListModel
import com.vectordemo.domain.model.UserSessionModel
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

class OssRemoteApiSource(private val apiClient: ApiClient) {
    suspend fun ossUserBucketList(userId: String): OssUserBucketListModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossUserBucketList(userId) },
            "存储桶列表响应为空",
        )
        return OssConvertor.bucketListResponseToModel(data)
    }

    suspend fun ossUserBucketFileItemList(userId: String, bucketName: String): OssBucketFileItemListModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossUserBucketFileItemList(userId, bucketName) },
            "文件明细列表响应为空",
        )
        return OssConvertor.fileItemListResponseToModel(data)
    }

    suspend fun ossBatchUploadSingle(
        userId: String,
        bucketName: String?,
        file: MultipartPartPayload,
    ): OssBatchUploadModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossBatchUpload(userId, bucketName, listOf(file)) },
            "上传响应为空",
        )
        return OssConvertor.batchUploadResponseToModel(data)
    }

    suspend fun ossBatchDelete(fileIds: List<String>): OssBatchDeleteModel {
        if (fileIds.isEmpty()) throw NetworkParamIllegalException("fileIds 为空")
        val data = RemoteRequestData.requestData(
            { apiClient.ossBatchDelete(fileIds) },
            "删除响应为空",
        )
        return OssConvertor.batchDeleteResponseToModel(data)
    }

    suspend fun ossUpdateFileContent(fileId: String, file: MultipartPartPayload): OssFileContentUpdateModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossUpdateFileContent(fileId, file) },
            "更新文件内容响应为空",
        )
        return OssConvertor.fileContentUpdateResponseToModel(data)
    }
}
