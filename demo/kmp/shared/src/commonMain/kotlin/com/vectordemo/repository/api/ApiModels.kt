package com.vectordemo.repository.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val code: String? = null,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class UserLoginRequest(
    val account: String = "",
    val password: String = "",
)

@Serializable
data class UserPasswordUpdateRequest(
    val userId: String? = null,
    val oldPassword: String? = null,
    val newPassword: String? = null,
)

@Serializable
data class UserTokenVerifyRequest(
    val userId: String? = null,
    val accessToken: String? = null,
)

@Serializable
data class UserAuthResponse(
    val userId: String? = null,
    val account: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val accessToken: String? = null,
)

@Serializable
data class UserTokenVerifyResponse(
    val userId: String? = null,
    val valid: Boolean? = null,
    val message: String? = null,
)

@Serializable
data class UserPasswordUpdateResponse(
    val userId: String? = null,
    val updated: Boolean? = null,
    val message: String? = null,
)

@Serializable
data class OssUserBucketListResponse(
    val userId: String? = null,
    val bucketNameList: List<String>? = null,
)

@Serializable
data class OssUserBucketFileIdsResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val fileIdList: List<String>? = null,
)

@Serializable
data class OssUserBucketFileUrlsResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val urlList: List<String>? = null,
)

@Serializable
data class OssUserBucketFileItemListResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val items: List<OssUserBucketFileItemRow>? = null,
)

@Serializable
data class OssUserBucketFileItemRow(
    val fileId: String? = null,
    val originFileName: String? = null,
    val url: String? = null,
)

@Serializable
data class OssUploadItemResult(
    val originFileName: String? = null,
    val success: Boolean = false,
    val duplicated: Boolean = false,
    val fileId: String? = null,
    val url: String? = null,
    val message: String? = null,
)

@Serializable
data class OssBatchUploadResponse(
    val userId: String? = null,
    val bucketName: String? = null,
    val successCount: Int? = null,
    val failCount: Int? = null,
    val items: List<OssUploadItemResult>? = null,
)

@Serializable
data class OssBatchDeleteResponse(
    val fileIdList: List<String>? = null,
    val successCount: Int? = null,
    val failCount: Int? = null,
    val message: String? = null,
)

@Serializable
data class OssFileContentUpdateResponse(
    val fileId: String? = null,
    val originFileName: String? = null,
    val url: String? = null,
    val updated: Boolean? = null,
    val message: String? = null,
)

@Serializable
data class MultipartPartPayload(
    val fieldName: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)
