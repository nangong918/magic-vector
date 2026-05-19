package com.vectordemo.dataSource.local

import com.vectordemo.database.Oss_user_bucket_file
import com.vectordemo.database.VectorDatabase
import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.entity.OssUserBucketFileEntity
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.domain.platform.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class OssLocalSource(
    private val database: VectorDatabase,
) {
    private val queries get() = database.ossUserBucketFileQueries

    suspend fun syncBucketNamesFromNetwork(userId: Long, bucketNames: List<String>, syncedAt: Long) =
        withContext(Dispatchers.IO) {
            val old = queries.distinctBuckets(userId).executeAsList().toSet()
            val new = bucketNames.toSet()
            val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
            for (b in old - new) {
                queries.deleteAllForUserBucket(userId, b)
            }
            for (b in new - old) {
                upsertEntity(
                    OssUserBucketFileEntity(userId, b, ph, fileName = "", fileUrl = "", syncedAt = syncedAt),
                )
            }
        }

    suspend fun replaceBucketFilesFromItemEntities(
        userId: Long,
        bucketName: String,
        entities: List<OssUserBucketFileEntity>,
        syncedAt: Long,
    ) = withContext(Dispatchers.IO) {
        val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
        queries.deleteAllForUserBucket(userId, bucketName)
        if (entities.isEmpty()) {
            upsertEntity(
                OssUserBucketFileEntity(userId, bucketName, ph, fileName = "", fileUrl = "", syncedAt = syncedAt),
            )
        } else {
            entities.forEach { upsertEntity(it) }
        }
    }

    suspend fun listCachedBucketNames(userId: Long): List<String> =
        withContext(Dispatchers.IO) { queries.distinctBuckets(userId).executeAsList() }

    suspend fun listDisplayFileModels(userId: Long, bucketName: String): List<OssBucketFileItemModel> =
        withContext(Dispatchers.IO) {
            val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
            queries.rowsInBucket(userId, bucketName).executeAsList()
                .filter { it.file_id != ph }
                .map { OssConvertor.entityToItemModel(it.toEntity()) }
        }

    suspend fun deleteFileRows(userId: Long, bucketName: String, fileIds: List<Long>) =
        withContext(Dispatchers.IO) {
            if (fileIds.isEmpty()) return@withContext
            val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
            for (fid in fileIds) {
                if (fid == 0L) continue
                val key = fid.toString()
                if (key == ph) continue
                queries.deleteRow(userId, bucketName, key)
            }
            if (queries.countRealFiles(userId, bucketName, ph).executeAsOne() == 0L) {
                upsertEntity(
                    OssUserBucketFileEntity(
                        userId,
                        bucketName,
                        ph,
                        fileName = "",
                        fileUrl = "",
                        syncedAt = currentTimeMillis(),
                    ),
                )
            }
        }

    private fun upsertEntity(entity: OssUserBucketFileEntity) {
        queries.upsertOssRow(
            user_id = entity.userId,
            bucket_name = entity.bucketName,
            file_id = entity.fileId,
            file_name = entity.fileName,
            file_url = entity.fileUrl,
            synced_at = entity.syncedAt,
        )
    }

    private fun Oss_user_bucket_file.toEntity(): OssUserBucketFileEntity = OssUserBucketFileEntity(
        userId = user_id,
        bucketName = bucket_name,
        fileId = file_id,
        fileName = file_name,
        fileUrl = file_url,
        syncedAt = synced_at,
    )
}
