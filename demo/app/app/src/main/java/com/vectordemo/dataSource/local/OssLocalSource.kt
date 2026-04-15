package com.vectordemo.dataSource.local

import android.content.Context
import com.vectordemo.dataSource.local.db.VectorDatabase
import com.vectordemo.domain.convertor.OssConvertor
import com.vectordemo.domain.entity.OssUserBucketFileEntity
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.repository.dao.OssUserBucketFileDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OssLocalSource private constructor(context: Context) {
    private val dao: OssUserBucketFileDao = VectorDatabase.getInstance(context).ossUserBucketFileDao()

    suspend fun syncBucketNamesFromNetwork(userId: Long, bucketNames: List<String>, syncedAt: Long) =
        withContext(Dispatchers.IO) {
            val old = dao.distinctBuckets(userId).toSet()
            val new = bucketNames.toSet()
            val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
            for (b in old - new) {
                dao.deleteAllForUserBucket(userId, b)
            }
            for (b in new - old) {
                dao.upsertAll(
                    listOf(OssUserBucketFileEntity(userId, b, ph, fileName = "", fileUrl = "", syncedAt = syncedAt))
                )
            }
        }

    suspend fun replaceBucketFilesFromItemEntities(
        userId: Long,
        bucketName: String,
        entities: List<OssUserBucketFileEntity>,
        syncedAt: Long
    ) = withContext(Dispatchers.IO) {
        val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
        dao.deleteAllForUserBucket(userId, bucketName)
        if (entities.isEmpty()) {
            dao.upsertAll(
                listOf(OssUserBucketFileEntity(userId, bucketName, ph, fileName = "", fileUrl = "", syncedAt = syncedAt))
            )
        } else {
            dao.upsertAll(entities)
        }
    }

    suspend fun listCachedBucketNames(userId: Long): List<String> =
        withContext(Dispatchers.IO) { dao.distinctBuckets(userId) }

    suspend fun listDisplayFileModels(userId: Long, bucketName: String): List<OssBucketFileItemModel> =
        withContext(Dispatchers.IO) {
            val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
            dao.rowsInBucket(userId, bucketName)
                .filter { it.fileId != ph }
                .map { OssConvertor.entityToItemModel(it) }
        }

    suspend fun deleteFileRows(userId: Long, bucketName: String, fileIds: List<Long>) =
        withContext(Dispatchers.IO) {
            if (fileIds.isEmpty()) return@withContext
            val ph = OssUserBucketFileEntity.EMPTY_BUCKET_FILE_ID
            for (fid in fileIds) {
                if (fid == 0L) continue
                val key = fid.toString()
                if (key == ph) continue
                dao.deleteRow(userId, bucketName, key)
            }
            if (dao.countRealFiles(userId, bucketName, ph) == 0) {
                dao.upsertAll(
                    listOf(
                        OssUserBucketFileEntity(
                            userId,
                            bucketName,
                            ph,
                            fileName = "",
                            fileUrl = "",
                            syncedAt = System.currentTimeMillis()
                        )
                    )
                )
            }
        }

    companion object {
        @Volatile
        private var INSTANCE: OssLocalSource? = null
        fun getInstance(context: Context): OssLocalSource {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OssLocalSource(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
