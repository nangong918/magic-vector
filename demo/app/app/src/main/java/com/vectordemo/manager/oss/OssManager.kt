package com.vectordemo.manager.oss

import android.content.Context
import com.vectordemo.MainApplication
import com.vectordemo.dataSource.local.OssLocalSource
import com.vectordemo.dataSource.remote.OssRemoteApiSource
import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileIdListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.domain.model.oss.OssBucketFileUrlListModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import java.io.File

/**
 * 在 [OssRemoteApiSource] 返回 Model 后写入 [com.vectordemo.domain.entity.OssUserBucketFileEntity]。
 * 「仅 URL 列表」接口响应不含 fileId，无法写入本表主键，故 [syncBucketFileUrlList] 只返回 Model、不落库。
 */
class OssManager private constructor(context: Context) {
    private val remote: OssRemoteApiSource = MainApplication.getOssRemoteApiSource()
    private val local: OssLocalSource = OssLocalSource.getInstance(context)

    suspend fun syncUserBucketList(userId: Long): OssUserBucketListModel {
        val model = remote.ossUserBucketList(userId.toString())
        if (model.userId > 0L) {
            val at = System.currentTimeMillis()
            local.syncBucketNamesFromNetwork(model.userId, model.bucketNames, at)
        }
        return model
    }

    suspend fun syncBucketFileItemList(userId: Long, bucketName: String): OssBucketFileItemListModel {
        val model = remote.ossUserBucketFileItemList(userId.toString(), bucketName)
        if (model.userId > 0L && model.bucketName.isNotBlank()) {
            val at = System.currentTimeMillis()
            val entities = OssConvertor.itemListModelToEntities(model, at)
            local.replaceBucketFilesFromItemEntities(model.userId, model.bucketName, entities, at)
        }
        return model
    }

    suspend fun syncBucketFileIdList(userId: Long, bucketName: String): OssBucketFileIdListModel {
        val model = remote.ossUserBucketFileIdList(userId.toString(), bucketName)
        if (model.userId > 0L && model.bucketName.isNotBlank()) {
            val at = System.currentTimeMillis()
            val entities = OssConvertor.idListModelToEntities(model, at)
            local.replaceBucketFilesFromItemEntities(model.userId, model.bucketName, entities, at)
        }
        return model
    }

    suspend fun syncBucketFileUrlList(userId: Long, bucketName: String): OssBucketFileUrlListModel =
        remote.ossUserBucketFileUrlList(userId.toString(), bucketName)

    suspend fun getCachedUserBucketList(userId: Long): OssUserBucketListModel? = withContext(Dispatchers.IO) {
        val names = local.listCachedBucketNames(userId)
        if (names.isEmpty()) null else OssUserBucketListModel(userId = userId, bucketNames = names)
    }

    suspend fun getCachedBucketFileItems(userId: Long, bucketName: String): List<OssBucketFileItemModel> =
        withContext(Dispatchers.IO) { local.listDisplayFileModels(userId, bucketName) }

    suspend fun batchUploadSingle(
        userId: Long,
        bucketName: String?,
        file: File,
        uploadFilename: String,
        mimeType: String
    ): OssBatchUploadModel =
        remote.ossBatchUploadSingle(userId.toString(), bucketName, file, uploadFilename, mimeType)

    suspend fun batchDelete(fileIds: List<Long>, userId: Long, bucketName: String): OssBatchDeleteModel {
        val wireIds = fileIds.map { it.toString() }
        val model = remote.ossBatchDelete(wireIds)
        local.deleteFileRows(userId, bucketName, fileIds)
        return model
    }

    suspend fun updateFileContent(
        fileId: String,
        fileBody: RequestBody,
        uploadFilename: String
    ): OssFileContentUpdateModel =
        remote.ossUpdateFileContent(fileId, fileBody, uploadFilename)

    companion object {
        @Volatile
        private var INSTANCE: OssManager? = null
        fun getInstance(context: Context): OssManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OssManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
