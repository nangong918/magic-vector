package com.vectordemo.manager.oss

import com.vectordemo.dataSource.local.OssLocalSource
import com.vectordemo.dataSource.remote.OssRemoteApiSource
import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
import com.vectordemo.domain.platform.currentTimeMillis
import com.vectordemo.repository.api.MultipartPartPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class OssManager(
    private val remote: OssRemoteApiSource,
    private val local: OssLocalSource,
) {
    suspend fun syncUserBucketList(userId: Long): OssUserBucketListModel {
        val model = remote.ossUserBucketList(userId.toString())
        if (model.userId > 0L) {
            local.syncBucketNamesFromNetwork(model.userId, model.bucketNames, currentTimeMillis())
        }
        return model
    }

    suspend fun syncBucketFileItemList(userId: Long, bucketName: String): OssBucketFileItemListModel {
        val model = remote.ossUserBucketFileItemList(userId.toString(), bucketName)
        if (model.userId > 0L && model.bucketName.isNotBlank()) {
            val at = currentTimeMillis()
            val entities = OssConvertor.itemListModelToEntities(model, at)
            local.replaceBucketFilesFromItemEntities(model.userId, model.bucketName, entities, at)
        }
        return model
    }

    suspend fun getCachedUserBucketList(userId: Long): OssUserBucketListModel? = withContext(Dispatchers.IO) {
        val names = local.listCachedBucketNames(userId)
        if (names.isEmpty()) null else OssUserBucketListModel(userId = userId, bucketNames = names)
    }

    suspend fun getCachedBucketFileItems(userId: Long, bucketName: String): List<OssBucketFileItemModel> =
        withContext(Dispatchers.IO) { local.listDisplayFileModels(userId, bucketName) }

    suspend fun batchUploadSingle(
        userId: Long,
        bucketName: String?,
        file: MultipartPartPayload,
    ): OssBatchUploadModel = remote.ossBatchUploadSingle(userId.toString(), bucketName, file)

    suspend fun batchDelete(fileIds: List<Long>, userId: Long, bucketName: String): OssBatchDeleteModel {
        val model = remote.ossBatchDelete(fileIds.map { it.toString() })
        local.deleteFileRows(userId, bucketName, fileIds)
        return model
    }

    suspend fun updateFileContent(fileId: String, file: MultipartPartPayload): OssFileContentUpdateModel =
        remote.ossUpdateFileContent(fileId, file)
}
