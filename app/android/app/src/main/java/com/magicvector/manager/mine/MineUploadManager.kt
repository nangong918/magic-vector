package com.magicvector.manager.mine

import android.content.ContentResolver
import android.net.Uri
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.magicvector.domain.dto.http.request.VideoUploadCompleteRequest
import com.magicvector.domain.dto.http.request.VideoUploadInitRequest
import com.magicvector.domain.dto.http.response.VideoUploadChunkResponse
import com.magicvector.domain.dto.http.response.VideoUploadCompleteResponse
import com.magicvector.domain.dto.http.response.VideoUploadInitResponse
import com.magicvector.MainApplication
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class MineUploadManager {
    private val api = MainApplication.getRemoteApiSource()

    fun createUploadSession(
        userId: String,
        fileName: String,
        fileSize: Long,
        onSuccess: (VideoUploadInitResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        val request = VideoUploadInitRequest().apply {
            this.userId = userId
            this.fileName = fileName
            this.fileSize = fileSize
        }
        api.initVideoUpload(
            request = request,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<VideoUploadInitResponse>> {
                override fun onResponse(response: BaseResponse<VideoUploadInitResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }

    fun uploadChunk(
        uploadId: String,
        userId: String,
        chunkIndex: Int,
        offset: Long,
        chunkFile: File,
        onSuccess: (VideoUploadChunkResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        val plain = "text/plain".toMediaTypeOrNull()
        val uploadIdBody = uploadId.toRequestBody(plain)
        val userIdBody = userId.toRequestBody(plain)
        val chunkIndexBody = chunkIndex.toString().toRequestBody(plain)
        val offsetBody = offset.toString().toRequestBody(plain)
        val chunkPart = MultipartBody.Part.createFormData(
            "chunkFile",
            chunkFile.name,
            chunkFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
        api.uploadVideoChunk(
            uploadId = uploadIdBody,
            userId = userIdBody,
            chunkIndex = chunkIndexBody,
            offset = offsetBody,
            chunkFile = chunkPart,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<VideoUploadChunkResponse>> {
                override fun onResponse(response: BaseResponse<VideoUploadChunkResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }

    fun completeUpload(
        uploadId: String,
        userId: String,
        onSuccess: (VideoUploadCompleteResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        val request = VideoUploadCompleteRequest().apply {
            this.uploadId = uploadId
            this.userId = userId
        }
        api.completeVideoUpload(
            request = request,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<VideoUploadCompleteResponse>> {
                override fun onResponse(response: BaseResponse<VideoUploadCompleteResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }

    fun copyUriToTempFile(contentResolver: ContentResolver, uri: Uri, fileName: String): File {
        val tempFile = File(MainApplication.getApp().cacheDir, "upload-$fileName")
        contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(tempFile).use { output ->
                input?.copyTo(output)
            }
        }
        return tempFile
    }
}
