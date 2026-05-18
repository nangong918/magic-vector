package com.vectordemo.repository.api

import com.vectordemo.domain.constant.BaseConstant
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.http.parameters

class ApiClient(
    private val http: HttpClient,
    private val baseUrl: String = BaseConstant.ConstantUrl.LOCAL_URL,
) {
    suspend fun register(
        avatar: MultipartPartPayload?,
        account: String,
        password: String,
        name: String,
    ): BaseResponse<UserAuthResponse> {
        return http.post("$baseUrl/user/register") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        if (avatar != null) {
                            append(
                                key = avatar.fieldName,
                                value = avatar.bytes,
                                headers = Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        ContentDisposition.File
                                            .withParameter(ContentDisposition.Parameters.Name, avatar.fieldName)
                                            .withParameter(ContentDisposition.Parameters.FileName, avatar.fileName)
                                            .toString(),
                                    )
                                    append(HttpHeaders.ContentType, avatar.mimeType)
                                },
                            )
                        }
                        append("account", account)
                        append("password", password)
                        append("name", name)
                    },
                ),
            )
        }.body()
    }

    suspend fun login(request: UserLoginRequest): BaseResponse<UserAuthResponse> {
        return http.post("$baseUrl/user/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun verifyAccessToken(request: UserTokenVerifyRequest): BaseResponse<UserTokenVerifyResponse> {
        return http.post("$baseUrl/user/token/verify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updatePassword(request: UserPasswordUpdateRequest): BaseResponse<UserPasswordUpdateResponse> {
        return http.post("$baseUrl/user/password/update") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun ossUserBucketList(userId: String): BaseResponse<OssUserBucketListResponse> {
        return http.submitForm(
            url = "$baseUrl/oss/user/bucket/list",
            formParameters = parameters { append("userId", userId) },
        ).body()
    }

    suspend fun ossUserBucketFileIdList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileIdsResponse> {
        return http.submitForm(
            url = "$baseUrl/oss/user/bucket/file/id/list",
            formParameters = parameters {
                append("userId", userId)
                append("bucketName", bucketName)
            },
        ).body()
    }

    suspend fun ossUserBucketFileUrlList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileUrlsResponse> {
        return http.submitForm(
            url = "$baseUrl/oss/user/bucket/file/url/list",
            formParameters = parameters {
                append("userId", userId)
                append("bucketName", bucketName)
            },
        ).body()
    }

    suspend fun ossUserBucketFileItemList(
        userId: String,
        bucketName: String,
    ): BaseResponse<OssUserBucketFileItemListResponse> {
        return http.submitForm(
            url = "$baseUrl/oss/user/bucket/file/item/list",
            formParameters = parameters {
                append("userId", userId)
                append("bucketName", bucketName)
            },
        ).body()
    }

    suspend fun ossBatchUpload(
        userId: String,
        bucketName: String?,
        files: List<MultipartPartPayload>,
    ): BaseResponse<OssBatchUploadResponse> {
        return http.post("$baseUrl/oss/upload/batch") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("userId", userId)
                        if (!bucketName.isNullOrBlank()) append("bucketName", bucketName)
                        files.forEach { file ->
                            append(
                                key = "files",
                                value = file.bytes,
                                headers = Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        ContentDisposition.File
                                            .withParameter(ContentDisposition.Parameters.Name, "files")
                                            .withParameter(ContentDisposition.Parameters.FileName, file.fileName)
                                            .toString(),
                                    )
                                    append(HttpHeaders.ContentType, file.mimeType)
                                },
                            )
                        }
                    },
                ),
            )
        }.body()
    }

    suspend fun ossBatchDelete(fileIds: List<String>): BaseResponse<OssBatchDeleteResponse> {
        return http.post("$baseUrl/oss/file/delete/batch") {
            setBody(
                FormDataContent(
                    parameters {
                        fileIds.forEach { append("fileIdList", it) }
                    },
                ),
            )
        }.body()
    }

    suspend fun ossUpdateFileContent(
        fileId: String,
        file: MultipartPartPayload,
    ): BaseResponse<OssFileContentUpdateResponse> {
        return http.post("$baseUrl/oss/file/content/update") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("fileId", fileId)
                        append(
                            key = "file",
                            value = file.bytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.File
                                        .withParameter(ContentDisposition.Parameters.Name, "file")
                                        .withParameter(ContentDisposition.Parameters.FileName, file.fileName)
                                        .toString(),
                                )
                                append(HttpHeaders.ContentType, file.mimeType)
                            },
                        )
                    },
                ),
            )
        }.body()
    }
}
