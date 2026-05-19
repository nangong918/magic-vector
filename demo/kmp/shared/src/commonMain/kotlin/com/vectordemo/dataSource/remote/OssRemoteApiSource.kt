package com.vectordemo.dataSource.remote

import com.vectordemo.domain.dto.http.request.MultipartPartPayload
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
import com.vectordemo.repository.api.ApiRequest

class OssRemoteApiSource(private val apiRequest: ApiRequest) {
    suspend fun ossUserBucketList(userId: String): OssUserBucketListModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUserBucketList(userId) },
            "存储桶列表响应为空",
        )
        return OssUserBucketListModel.fromResponse(data)
    }

    suspend fun ossUserBucketFileItemList(userId: String, bucketName: String): OssBucketFileItemListModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUserBucketFileItemList(userId, bucketName) },
            "文件明细列表响应为空",
        )
        return OssBucketFileItemListModel.fromResponse(data)
    }

    suspend fun ossBatchUploadSingle(
        userId: String,
        bucketName: String?,
        file: MultipartPartPayload,
    ): OssBatchUploadModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossBatchUpload(userId, bucketName, listOf(file)) },
            "上传响应为空",
        )
        return OssBatchUploadModel.fromResponse(data)
    }

    suspend fun ossBatchDelete(fileIds: List<String>): OssBatchDeleteModel {
        if (fileIds.isEmpty()) throw NetworkParamIllegalException("fileIds 为空")
        val data = RemoteRequestData.requestData(
            { apiRequest.ossBatchDelete(fileIds) },
            "删除响应为空",
        )
        return OssBatchDeleteModel.fromResponse(data)
    }

    suspend fun ossUpdateFileContent(fileId: String, file: MultipartPartPayload): OssFileContentUpdateModel {
        val data = RemoteRequestData.requestData(
            { apiRequest.ossUpdateFileContent(fileId, file) },
            "更新文件内容响应为空",
        )
        return OssFileContentUpdateModel.fromResponse(data)
    }
}
