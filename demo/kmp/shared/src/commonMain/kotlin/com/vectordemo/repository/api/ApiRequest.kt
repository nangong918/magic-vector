package com.vectordemo.repository.api

import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.dto.http.request.UserLoginRequest
import com.vectordemo.domain.dto.http.request.UserPasswordUpdateRequest
import com.vectordemo.domain.dto.http.request.UserTokenVerifyRequest
import com.vectordemo.domain.dto.http.response.BaseResponse
import com.vectordemo.domain.dto.http.response.OssBatchDeleteResponse
import com.vectordemo.domain.dto.http.response.OssBatchUploadResponse
import com.vectordemo.domain.dto.http.response.OssFileContentUpdateResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileIdsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemListResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileUrlsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketListResponse
import com.vectordemo.domain.dto.http.response.UserAuthResponse
import com.vectordemo.domain.dto.http.response.UserPasswordUpdateResponse
import com.vectordemo.domain.dto.http.response.UserTokenVerifyResponse

/**
 * 业务 HTTP 契约，与 demo/app 的 `ApiRequest` 方法签名对齐。
 *
 * - **Android 单端**：可用 Retrofit + OkHttp 生成实现（见 demo/app）。
 * - **KMP commonMain**：Retrofit 不支持多平台；使用 [ApiRequestImpl]（Ktor）或后续引入 **Ktorfit**（Retrofit 风格注解 + Ktor）。
 */
interface ApiRequest {
    suspend fun register(
        avatar: MultipartPartPayload?,
        account: String,
        password: String,
        name: String,
    ): BaseResponse<UserAuthResponse>

    suspend fun login(request: UserLoginRequest): BaseResponse<UserAuthResponse>

    suspend fun verifyAccessToken(request: UserTokenVerifyRequest): BaseResponse<UserTokenVerifyResponse>

    suspend fun updatePassword(request: UserPasswordUpdateRequest): BaseResponse<UserPasswordUpdateResponse>

    suspend fun ossUserBucketList(userId: String): BaseResponse<OssUserBucketListResponse>

    suspend fun ossUserBucketFileIdList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileIdsResponse>

    suspend fun ossUserBucketFileUrlList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileUrlsResponse>

    suspend fun ossUserBucketFileItemList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileItemListResponse>

    suspend fun ossBatchUpload(
        userId: String,
        bucketName: String?,
        files: List<MultipartPartPayload>,
    ): BaseResponse<OssBatchUploadResponse>

    suspend fun ossBatchDelete(fileIds: List<String>): BaseResponse<OssBatchDeleteResponse>

    suspend fun ossUpdateFileContent(
        fileId: String,
        file: MultipartPartPayload,
    ): BaseResponse<OssFileContentUpdateResponse>

    companion object {
        const val PATH_USER_REGISTER = "/user/register"
        const val PATH_USER_LOGIN = "/user/login"
        const val PATH_USER_TOKEN_VERIFY = "/user/token/verify"
        const val PATH_USER_PASSWORD_UPDATE = "/user/password/update"
        const val PATH_OSS_USER_BUCKET_LIST = "/oss/user/bucket/list"
        const val PATH_OSS_USER_BUCKET_FILE_ID_LIST = "/oss/user/bucket/file/id/list"
        const val PATH_OSS_USER_BUCKET_FILE_URL_LIST = "/oss/user/bucket/file/url/list"
        const val PATH_OSS_USER_BUCKET_FILE_ITEM_LIST = "/oss/user/bucket/file/item/list"
        const val PATH_OSS_UPLOAD_BATCH = "/oss/upload/batch"
        const val PATH_OSS_FILE_DELETE_BATCH = "/oss/file/delete/batch"
        const val PATH_OSS_FILE_CONTENT_UPDATE = "/oss/file/content/update"
    }
}
