package com.vectordemo.repository.api

import com.vectordemo.domain.constant.BaseConstant
import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.dto.http.request.UserLoginRequest
import com.vectordemo.domain.dto.http.request.UserPasswordUpdateRequest
import com.vectordemo.domain.dto.http.request.UserTokenVerifyRequest
import com.vectordemo.domain.dto.http.request.appendPayload
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
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
import com.vectordemo.domain.model.user.UserSessionModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters

class ApiRequestImpl(
    private val http: HttpClient,
    private val baseUrl: String = BaseConstant.ConstantUrl.LOCAL_URL,
) : ApiRequest {

    override suspend fun register(
        avatar: MultipartPartPayload?,
        account: String,
        password: String,
        name: String,
    ): BaseResponse<UserAuthResponse> =
        postMultipart(
            ApiRequest.PATH_USER_REGISTER,
            UserSessionModel.registerFormData(avatar, account, password, name),
        )

    override suspend fun login(request: UserLoginRequest): BaseResponse<UserAuthResponse> =
        postJson(ApiRequest.PATH_USER_LOGIN, request)

    override suspend fun verifyAccessToken(request: UserTokenVerifyRequest): BaseResponse<UserTokenVerifyResponse> =
        postJson(ApiRequest.PATH_USER_TOKEN_VERIFY, request)

    override suspend fun updatePassword(request: UserPasswordUpdateRequest): BaseResponse<UserPasswordUpdateResponse> =
        postJson(ApiRequest.PATH_USER_PASSWORD_UPDATE, request)

    override suspend fun ossUserBucketList(userId: String): BaseResponse<OssUserBucketListResponse> =
        submitForm(ApiRequest.PATH_OSS_USER_BUCKET_LIST, OssUserBucketListModel.formParameters(userId))

    override suspend fun ossUserBucketFileIdList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileIdsResponse> =
        submitForm(
            ApiRequest.PATH_OSS_USER_BUCKET_FILE_ID_LIST,
            parameters {
                append("userId", userId)
                append("bucketName", bucketName)
            },
        )

    override suspend fun ossUserBucketFileUrlList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileUrlsResponse> =
        submitForm(
            ApiRequest.PATH_OSS_USER_BUCKET_FILE_URL_LIST,
            parameters {
                append("userId", userId)
                append("bucketName", bucketName)
            },
        )

    override suspend fun ossUserBucketFileItemList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileItemListResponse> =
        submitForm(
            ApiRequest.PATH_OSS_USER_BUCKET_FILE_ITEM_LIST,
            OssBucketFileItemListModel.formParameters(userId, bucketName),
        )

    override suspend fun ossBatchUpload(
        userId: String,
        bucketName: String?,
        files: List<MultipartPartPayload>,
    ): BaseResponse<OssBatchUploadResponse> =
        postMultipart(
            ApiRequest.PATH_OSS_UPLOAD_BATCH,
            OssBatchUploadModel.multipartFormData(userId, bucketName, files),
        )

    override suspend fun ossBatchDelete(fileIds: List<String>): BaseResponse<OssBatchDeleteResponse> =
        postFormEncoded(ApiRequest.PATH_OSS_FILE_DELETE_BATCH, OssBatchDeleteModel.formParameters(fileIds))

    override suspend fun ossUpdateFileContent(
        fileId: String,
        file: MultipartPartPayload,
    ): BaseResponse<OssFileContentUpdateResponse> =
        postMultipart(
            ApiRequest.PATH_OSS_FILE_CONTENT_UPDATE,
            OssFileContentUpdateModel.multipartFormData(fileId, file),
        )

    private suspend inline fun <reified T> postJson(path: String, body: Any): BaseResponse<T> =
        http.post(url(path)) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    private suspend inline fun <reified T> submitForm(
        path: String,
        formParameters: io.ktor.http.Parameters,
    ): BaseResponse<T> =
        http.submitForm(url = url(path), formParameters = formParameters).body()

    private suspend inline fun <reified T> postMultipart(
        path: String,
        noinline configure: io.ktor.client.request.forms.FormBuilder.() -> Unit,
    ): BaseResponse<T> =
        http.post(url(path)) {
            setBody(MultiPartFormDataContent(formData(configure)))
        }.body()

    private suspend inline fun <reified T> postFormEncoded(
        path: String,
        parameters: io.ktor.http.Parameters,
    ): BaseResponse<T> =
        http.post(url(path)) {
            setBody(FormDataContent(parameters))
        }.body()

    private fun url(path: String): String = "$baseUrl$path"
}
