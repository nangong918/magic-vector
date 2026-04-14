package com.vectordemo.repository.api

import com.vectordemo.domain.dto.http.request.UserLoginRequest
import com.vectordemo.domain.dto.http.request.UserPasswordUpdateRequest
import com.vectordemo.domain.dto.http.request.UserTokenVerifyRequest
import com.vectordemo.domain.dto.http.response.OssBatchUploadResponse
import com.vectordemo.domain.dto.http.response.OssBatchDeleteResponse
import com.vectordemo.domain.dto.http.response.OssFileContentUpdateResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileIdsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemListResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileUrlsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketListResponse
import com.vectordemo.domain.dto.http.response.UserAuthResponse
import com.vectordemo.domain.dto.http.response.UserPasswordUpdateResponse
import com.vectordemo.domain.dto.http.response.UserTokenVerifyResponse
import com.vectordemo.utils.network.BaseResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiRequest {
    @Multipart
    @POST("/user/register")
    suspend fun register(
        @Part avatar: MultipartBody.Part?,
        @Part("account") account: RequestBody,
        @Part("password") password: RequestBody,
        @Part("name") name: RequestBody
    ): BaseResponse<UserAuthResponse>

    @POST("/user/login")
    suspend fun login(@Body request: UserLoginRequest): BaseResponse<UserAuthResponse>

    @POST("/user/token/verify")
    suspend fun verifyAccessToken(@Body request: UserTokenVerifyRequest): BaseResponse<UserTokenVerifyResponse>

    @POST("/user/password/update")
    suspend fun updatePassword(@Body request: UserPasswordUpdateRequest): BaseResponse<UserPasswordUpdateResponse>

    @FormUrlEncoded
    @POST("/oss/user/bucket/list")
    suspend fun ossUserBucketList(@Field("userId") userId: String): BaseResponse<OssUserBucketListResponse>

    @FormUrlEncoded
    @POST("/oss/user/bucket/file/id/list")
    suspend fun ossUserBucketFileIdList(
        @Field("userId") userId: String,
        @Field("bucketName") bucketName: String
    ): BaseResponse<OssUserBucketFileIdsResponse>

    @FormUrlEncoded
    @POST("/oss/user/bucket/file/url/list")
    suspend fun ossUserBucketFileUrlList(
        @Field("userId") userId: String,
        @Field("bucketName") bucketName: String
    ): BaseResponse<OssUserBucketFileUrlsResponse>

    @FormUrlEncoded
    @POST("/oss/user/bucket/file/item/list")
    suspend fun ossUserBucketFileItemList(
        @Field("userId") userId: String,
        @Field("bucketName") bucketName: String
    ): BaseResponse<OssUserBucketFileItemListResponse>

    @Multipart
    @POST("/oss/upload/batch")
    suspend fun ossBatchUpload(
        @Part("userId") userId: RequestBody,
        @Part("bucketName") bucketName: RequestBody?,
        @Part files: List<MultipartBody.Part>
    ): BaseResponse<OssBatchUploadResponse>

    @FormUrlEncoded
    @POST("/oss/file/delete/batch")
    suspend fun ossBatchDelete(
        @Field("fileIdList") fileIds: List<String>
    ): BaseResponse<OssBatchDeleteResponse>

    @Multipart
    @POST("/oss/file/content/update")
    suspend fun ossUpdateFileContent(
        @Part("fileId") fileId: RequestBody,
        @Part file: MultipartBody.Part
    ): BaseResponse<OssFileContentUpdateResponse>
}
