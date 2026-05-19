package com.vectordemo.dataSource.remote

import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.exception.NetworkParamIllegalException
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
import com.vectordemo.repository.api.ApiClient
import com.vectordemo.repository.api.MultipartPartPayload

class OssRemoteApiSource(private val apiClient: ApiClient) {
    suspend fun ossUserBucketList(userId: String): OssUserBucketListModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossUserBucketList(userId) },
            "存储桶列表响应为空",
        )
        return OssConvertor.bucketListResponseToModel(data)
    }

    suspend fun ossUserBucketFileItemList(userId: String, bucketName: String): OssBucketFileItemListModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossUserBucketFileItemList(userId, bucketName) },
            "文件明细列表响应为空",
        )
        return OssConvertor.fileItemListResponseToModel(data)
    }

    suspend fun ossBatchUploadSingle(
        userId: String,
        bucketName: String?,
        file: MultipartPartPayload,
    ): OssBatchUploadModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossBatchUpload(userId, bucketName, listOf(file)) },
            "上传响应为空",
        )
        return OssConvertor.batchUploadResponseToModel(data)
    }

    suspend fun ossBatchDelete(fileIds: List<String>): OssBatchDeleteModel {
        if (fileIds.isEmpty()) throw NetworkParamIllegalException("fileIds 为空")
        val data = RemoteRequestData.requestData(
            { apiClient.ossBatchDelete(fileIds) },
            "删除响应为空",
        )
        return OssConvertor.batchDeleteResponseToModel(data)
    }

    suspend fun ossUpdateFileContent(fileId: String, file: MultipartPartPayload): OssFileContentUpdateModel {
        val data = RemoteRequestData.requestData(
            { apiClient.ossUpdateFileContent(fileId, file) },
            "更新文件内容响应为空",
        )
        return OssConvertor.fileContentUpdateResponseToModel(data)
    }
}
