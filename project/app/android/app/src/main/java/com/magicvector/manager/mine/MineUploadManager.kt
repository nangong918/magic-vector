package com.magicvector.manager.mine

import android.content.ContentResolver
import android.net.Uri
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

    suspend fun createUploadSession(
        userId: String,
        fileName: String,
        fileSize: Long
    ): VideoUploadInitResponse {
        val request = VideoUploadInitRequest().apply {
            this.userId = userId
            this.fileName = fileName
            this.fileSize = fileSize
        }
        return api.initVideoUpload(request)
    }

    suspend fun uploadChunk(
        uploadId: String,
        userId: String,
        chunkIndex: Int,
        offset: Long,
        chunkFile: File
    ): VideoUploadChunkResponse {
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
        return api.uploadVideoChunk(
            uploadId = uploadIdBody,
            userId = userIdBody,
            chunkIndex = chunkIndexBody,
            offset = offsetBody,
            chunkFile = chunkPart
        )
    }

    suspend fun completeUpload(
        uploadId: String,
        userId: String
    ): VideoUploadCompleteResponse {
        val request = VideoUploadCompleteRequest().apply {
            this.uploadId = uploadId
            this.userId = userId
        }
        return api.completeVideoUpload(request)
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
