package com.vectordemo.domain.model.oss

import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.dto.http.response.OssFileContentUpdateResponse
import com.vectordemo.domain.dto.http.request.appendPayload
import io.ktor.client.request.forms.FormBuilder

data class OssFileContentUpdateModel(
    val fileId: String,
    val originFileName: String,
    val url: String,
    val updated: Boolean,
    val message: String,
) {
    companion object {
        fun multipartFormData(fileId: String, file: MultipartPartPayload): FormBuilder.() -> Unit = {
            append("fileId", fileId)
            appendPayload(file, formFieldName = "file")
        }

        fun fromResponse(response: OssFileContentUpdateResponse): OssFileContentUpdateModel =
            OssFileContentUpdateModel(
                fileId = response.fileId.orEmpty(),
                originFileName = response.originFileName.orEmpty(),
                url = response.url.orEmpty(),
                updated = response.updated == true,
                message = response.message.orEmpty(),
            )
    }
}
