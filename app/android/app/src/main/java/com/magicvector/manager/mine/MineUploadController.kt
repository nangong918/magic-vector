package com.magicvector.manager.mine

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class MineUploadController(
    private val uploadManager: MineUploadManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var uploadJob: Job? = null
    @Volatile
    private var paused = false

    fun pauseTask() {
        paused = true
    }

    fun resumeTask(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: String,
        userId: String,
        callback: (MineUploadProgress) -> Unit
    ) {
        paused = false
        scheduleUpload(contentResolver, uri, fileName, userId, callback)
    }

    fun scheduleUpload(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: String,
        userId: String,
        callback: (MineUploadProgress) -> Unit
    ) {
        uploadJob?.cancel()
        uploadJob = scope.launch {
            val sourceFile = uploadManager.copyUriToTempFile(contentResolver, uri, fileName)
            try {
                val init = uploadManager.createUploadSession(
                    userId = userId,
                    fileName = fileName,
                    fileSize = sourceFile.length()
                )
                if (init.uploadId.isNullOrBlank()) {
                    callback.invoke(MineUploadProgress(error = "创建上传会话失败"))
                    return@launch
                }
                startUploadChunks(
                    sourceFile = sourceFile,
                    uploadId = init.uploadId,
                    userId = userId,
                    offset = init.uploadedOffset ?: 0L,
                    chunkSize = init.chunkSize ?: 5 * 1024 * 1024,
                    callback = callback
                )
            } catch (_: Throwable) {
                callback.invoke(MineUploadProgress(error = "创建上传会话失败"))
            }
        }
    }

    private suspend fun startUploadChunks(
        sourceFile: File,
        uploadId: String,
        userId: String,
        offset: Long,
        chunkSize: Int,
        callback: (MineUploadProgress) -> Unit
    ) {
        var currentOffset = offset
        var chunkIndex = (offset / chunkSize).toInt()
        while (currentOffset < sourceFile.length()) {
            if (paused) {
                callback.invoke(MineUploadProgress(uploadedBytes = currentOffset, totalBytes = sourceFile.length(), paused = true))
                return
            }
            val size = minOf(chunkSize.toLong(), sourceFile.length() - currentOffset).toInt()
            val chunkFile = File(sourceFile.parentFile, "chunk-$chunkIndex.tmp")
            sourceFile.inputStream().use { input ->
                input.skip(currentOffset)
                chunkFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var remaining = size
                    while (remaining > 0) {
                        val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                        if (read <= 0) {
                            break
                        }
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            }
            try {
                val chunk = uploadManager.uploadChunk(
                    uploadId = uploadId,
                    userId = userId,
                    chunkIndex = chunkIndex,
                    offset = currentOffset,
                    chunkFile = chunkFile
                )
                currentOffset = chunk.uploadedOffset ?: currentOffset
                callback.invoke(MineUploadProgress(uploadedBytes = currentOffset, totalBytes = sourceFile.length()))
            } catch (_: Throwable) {
                callback.invoke(
                    MineUploadProgress(
                        uploadedBytes = currentOffset,
                        totalBytes = sourceFile.length(),
                        error = "上传分片失败"
                    )
                )
            }
            //noinspection ResultOfMethodCallIgnored
            chunkFile.delete()
            chunkIndex += 1
        }
        try {
            uploadManager.completeUpload(
                uploadId = uploadId,
                userId = userId
            )
            callback.invoke(
                MineUploadProgress(
                    uploadedBytes = sourceFile.length(),
                    totalBytes = sourceFile.length(),
                    completed = true
                )
            )
        } catch (_: Throwable) {
            callback.invoke(
                MineUploadProgress(
                    uploadedBytes = currentOffset,
                    totalBytes = sourceFile.length(),
                    error = "完成上传失败"
                )
            )
        }
    }
}

data class MineUploadProgress(
    val uploadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val paused: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null
)
