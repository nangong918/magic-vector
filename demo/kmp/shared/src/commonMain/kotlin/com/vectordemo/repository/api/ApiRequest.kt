package com.vectordemo.repository.api

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
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.POST
import io.ktor.client.request.forms.MultiPartFormDataContent

/**
 * KSP（Ktorfit）在编译期生成实现类，对齐 demo/app Retrofit [ApiRequest] 写法。
 */
interface ApiRequest {
    @POST("user/register")
    suspend fun register(@Body body: MultiPartFormDataContent): BaseResponse<UserAuthResponse>

    @POST("user/login")
    suspend fun login(@Body request: UserLoginRequest): BaseResponse<UserAuthResponse>

    @POST("user/token/verify")
    suspend fun verifyAccessToken(@Body request: UserTokenVerifyRequest): BaseResponse<UserTokenVerifyResponse>

    @POST("user/password/update")
    suspend fun updatePassword(@Body request: UserPasswordUpdateRequest): BaseResponse<UserPasswordUpdateResponse>

    @FormUrlEncoded
    @POST("oss/user/bucket/list")
    suspend fun ossUserBucketList(@Field("userId") userId: String): BaseResponse<OssUserBucketListResponse>

    @FormUrlEncoded
    @POST("oss/user/bucket/file/id/list")
    suspend fun ossUserBucketFileIdList(
        @Field("userId") userId: String,
        @Field("bucketName") bucketName: String,
    ): BaseResponse<OssUserBucketFileIdsResponse>

    @FormUrlEncoded
    @POST("oss/user/bucket/file/url/list")
    suspend fun ossUserBucketFileUrlList(
        @Field("userId") userId: String,
        @Field("bucketName") bucketName: String,
    ): BaseResponse<OssUserBucketFileUrlsResponse>

    @FormUrlEncoded
    @POST("oss/user/bucket/file/item/list")
    suspend fun ossUserBucketFileItemList(
        @Field("userId") userId: String,
        @Field("bucketName") bucketName: String,
    ): BaseResponse<OssUserBucketFileItemListResponse>

    @POST("oss/upload/batch")
    suspend fun ossBatchUpload(@Body body: MultiPartFormDataContent): BaseResponse<OssBatchUploadResponse>

    @FormUrlEncoded
    @POST("oss/file/delete/batch")
    suspend fun ossBatchDelete(@Field("fileIdList") fileIds: List<String>): BaseResponse<OssBatchDeleteResponse>

    @POST("oss/file/content/update")
    suspend fun ossUpdateFileContent(@Body body: MultiPartFormDataContent): BaseResponse<OssFileContentUpdateResponse>
}
