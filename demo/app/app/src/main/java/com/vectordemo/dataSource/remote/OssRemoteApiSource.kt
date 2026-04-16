package com.vectordemo.dataSource.remote

import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileIdListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssBucketFileUrlListModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.repository.api.ApiRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * OSS 域远程数据源：不对外暴露 Response，仅返回业务 Model。
 */
class OssRemoteApiSource(private val apiRequest: ApiRequest) {

    suspend fun ossUserBucketList(userId: String): OssUserBucketListModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUserBucketList(userId) },
            "存储桶列表响应为空",
        )
        return OssConvertor.bucketListResponseToModel(data)
    }

    suspend fun ossUserBucketFileIdList(userId: String, bucketName: String): OssBucketFileIdListModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUserBucketFileIdList(userId, bucketName) },
            "文件 id 列表响应为空",
        )
        return OssConvertor.fileIdsResponseToModel(data)
    }

    suspend fun ossUserBucketFileUrlList(userId: String, bucketName: String): OssBucketFileUrlListModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUserBucketFileUrlList(userId, bucketName) },
            "文件 URL 列表响应为空",
        )
        return OssConvertor.fileUrlsResponseToModel(data)
    }

    suspend fun ossUserBucketFileItemList(userId: String, bucketName: String): OssBucketFileItemListModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUserBucketFileItemList(userId, bucketName) },
            "文件明细列表响应为空",
        )
        return OssConvertor.fileItemListResponseToModel(data)
    }

    suspend fun ossBatchUploadSingle(
        userId: String,
        bucketName: String?,
        file: File,
        uploadFilename: String,
        mimeType: String,
    ): OssBatchUploadModel {
        val uidBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
        val bucketBody = bucketName?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
        val media = (mimeType.ifBlank { "application/octet-stream" }).toMediaTypeOrNull()
        val body = file.asRequestBody(media)
        val part = MultipartBody.Part.createFormData("files", uploadFilename, body)
        val data = RemoteRequestData.requestData(
            { apiRequest.ossBatchUpload(uidBody, bucketBody, listOf(part)) },
            "上传响应为空",
        )
        return OssConvertor.batchUploadResponseToModel(data)
    }

    suspend fun ossBatchDelete(fileIds: List<String>): OssBatchDeleteModel {
        if (fileIds.isEmpty()) {
            throw NetworkParamIllegalException("fileIds 为空")
        }
        val data = RemoteRequestData.requestData(
            { apiRequest.ossBatchDelete(fileIds) },
            "删除响应为空",
        )
        return OssConvertor.batchDeleteResponseToModel(data)
    }

    suspend fun ossUpdateFileContent(
        fileId: String,
        fileBody: RequestBody,
        uploadFilename: String,
    ): OssFileContentUpdateModel {
        val fidBody = fileId.toRequestBody("text/plain".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", uploadFilename, fileBody)
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUpdateFileContent(fidBody, part) },
            "更新文件内容响应为空",
        )
        return OssConvertor.fileContentUpdateResponseToModel(data)
    }
}
