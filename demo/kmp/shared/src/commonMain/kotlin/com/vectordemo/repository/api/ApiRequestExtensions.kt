package com.vectordemo.repository.api

import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.dto.http.response.BaseResponse
import com.vectordemo.domain.dto.http.response.OssBatchUploadResponse
import com.vectordemo.domain.dto.http.response.OssFileContentUpdateResponse
import com.vectordemo.domain.dto.http.response.UserAuthResponse
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.user.UserSessionModel
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData

/** 由 Model 组装 multipart，避免在 Remote 层重复 formData 逻辑。 */
suspend fun ApiRequest.register(
    avatar: MultipartPartPayload?,
    account: String,
    password: String,
    name: String,
): BaseResponse<UserAuthResponse> =
    register(
        MultiPartFormDataContent(
            formData(UserSessionModel.registerFormData(avatar, account, password, name)),
        ),
    )

suspend fun ApiRequest.ossBatchUpload(
    userId: String,
    bucketName: String?,
    files: List<MultipartPartPayload>,
): BaseResponse<OssBatchUploadResponse> =
    ossBatchUpload(
        MultiPartFormDataContent(
            formData(OssBatchUploadModel.multipartFormData(userId, bucketName, files)),
        ),
    )

suspend fun ApiRequest.ossUpdateFileContent(
    fileId: String,
    file: MultipartPartPayload,
): BaseResponse<OssFileContentUpdateResponse> =
    ossUpdateFileContent(
        MultiPartFormDataContent(
            formData(OssFileContentUpdateModel.multipartFormData(fileId, file)),
        ),
    )
