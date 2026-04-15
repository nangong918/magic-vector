package com.vectordemo.dataSource.remote

import com.vectordemo.MainApplication
import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.domain.dto.http.request.UserLoginRequest
import com.vectordemo.domain.dto.http.request.UserPasswordUpdateRequest
import com.vectordemo.domain.dto.http.request.UserTokenVerifyRequest
import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileIdListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssBucketFileUrlListModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

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

    suspend fun ossUserBucketList(userId: String): OssUserBucketListModel {
        val data = requestData({ apiRequest.ossUserBucketList(userId) }, "存储桶列表响应为空")
        return OssConvertor.bucketListResponseToModel(data)
    }

    suspend fun ossUserBucketFileIdList(userId: String, bucketName: String): OssBucketFileIdListModel {
        val data = requestData(
            { apiRequest.ossUserBucketFileIdList(userId, bucketName) },
            "文件 id 列表响应为空"
        )
        return OssConvertor.fileIdsResponseToModel(data)
    }

    suspend fun ossUserBucketFileUrlList(userId: String, bucketName: String): OssBucketFileUrlListModel {
        val data = requestData(
            { apiRequest.ossUserBucketFileUrlList(userId, bucketName) },
            "文件 URL 列表响应为空"
        )
        return OssConvertor.fileUrlsResponseToModel(data)
    }

    suspend fun ossUserBucketFileItemList(userId: String, bucketName: String): OssBucketFileItemListModel {
        val data = requestData(
            { apiRequest.ossUserBucketFileItemList(userId, bucketName) },
            "文件明细列表响应为空"
        )
        return OssConvertor.fileItemListResponseToModel(data)
    }

    suspend fun ossBatchUploadSingle(
        userId: String,
        bucketName: String?,
        file: File,
        uploadFilename: String,
        mimeType: String
    ): OssBatchUploadModel {
        val uidBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
        val bucketBody = bucketName?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
        val media = (mimeType.ifBlank { "application/octet-stream" }).toMediaTypeOrNull()
        val body = file.asRequestBody(media)
        val part = MultipartBody.Part.createFormData("files", uploadFilename, body)
        val data = requestData(
            { apiRequest.ossBatchUpload(uidBody, bucketBody, listOf(part)) },
            "上传响应为空"
        )
        return OssConvertor.batchUploadResponseToModel(data)
    }

    suspend fun ossBatchDelete(fileIds: List<String>): OssBatchDeleteModel {
        if (fileIds.isEmpty()) {
            throw NetworkParamIllegalException("fileIds 为空")
        }
        val data = requestData({ apiRequest.ossBatchDelete(fileIds) }, "删除响应为空")
        return OssConvertor.batchDeleteResponseToModel(data)
    }

    suspend fun ossUpdateFileContent(
        fileId: String,
        fileBody: RequestBody,
        uploadFilename: String
    ): OssFileContentUpdateModel {
        val fidBody = fileId.toRequestBody("text/plain".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", uploadFilename, fileBody)
        val data = requestData(
            { apiRequest.ossUpdateFileContent(fidBody, part) },
            "更新文件内容响应为空"
        )
        return OssConvertor.fileContentUpdateResponseToModel(data)
    }
}
