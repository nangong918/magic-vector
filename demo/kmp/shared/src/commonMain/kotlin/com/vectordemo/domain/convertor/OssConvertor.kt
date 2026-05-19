package com.vectordemo.domain.convertor

import com.vectordemo.domain.entity.OssUserBucketFileEntity
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUploadItemModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel
import com.vectordemo.repository.api.OssBatchDeleteResponse
import com.vectordemo.repository.api.OssBatchUploadResponse
import com.vectordemo.repository.api.OssFileContentUpdateResponse
import com.vectordemo.repository.api.OssUploadItemResult
import com.vectordemo.repository.api.OssUserBucketFileItemListResponse
import com.vectordemo.repository.api.OssUserBucketFileItemRow
import com.vectordemo.repository.api.OssUserBucketListResponse

object OssConvertor {
    private fun wireFileIdToLong(raw: String?): Long =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L

    fun bucketListResponseToModel(r: OssUserBucketListResponse): OssUserBucketListModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        return OssUserBucketListModel(userId = uid, bucketNames = r.bucketNameList.orEmpty())
    }

    fun fileItemListResponseToModel(r: OssUserBucketFileItemListResponse): OssBucketFileItemListModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        val items = r.items.orEmpty().map { rowToItemModel(it) }
        return OssBucketFileItemListModel(userId = uid, bucketName = r.bucketName.orEmpty(), items = items)
    }

    fun rowToItemModel(row: OssUserBucketFileItemRow): OssBucketFileItemModel = OssBucketFileItemModel(
        fileId = wireFileIdToLong(row.fileId),
        originFileName = row.originFileName.orEmpty(),
        url = row.url.orEmpty(),
    )

    fun itemModelToEntity(
        model: OssBucketFileItemModel,
        userId: Long,
        bucketName: String,
        syncedAt: Long,
    ): OssUserBucketFileEntity = OssUserBucketFileEntity(
        userId = userId,
        bucketName = bucketName,
        fileId = model.fileId.toString(),
        fileName = model.originFileName,
        fileUrl = model.url,
        syncedAt = syncedAt,
    )

    fun itemListModelToEntities(model: OssBucketFileItemListModel, syncedAt: Long): List<OssUserBucketFileEntity> =
        model.items.map { itemModelToEntity(it, model.userId, model.bucketName, syncedAt) }

    fun entityToItemModel(entity: OssUserBucketFileEntity): OssBucketFileItemModel = OssBucketFileItemModel(
        fileId = wireFileIdToLong(entity.fileId),
        originFileName = entity.fileName,
        url = entity.fileUrl,
    )

    fun batchUploadResponseToModel(r: OssBatchUploadResponse): OssBatchUploadModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        val items = r.items.orEmpty().map { itemToModel(it) }
        return OssBatchUploadModel(
            userId = uid,
            bucketName = r.bucketName.orEmpty(),
            successCount = r.successCount ?: 0,
            failCount = r.failCount ?: 0,
            items = items,
        )
    }

    private fun itemToModel(it: OssUploadItemResult): OssUploadItemModel = OssUploadItemModel(
        originFileName = it.originFileName.orEmpty(),
        success = it.success,
        duplicated = it.duplicated,
        fileId = it.fileId.orEmpty(),
        url = it.url.orEmpty(),
        message = it.message.orEmpty(),
    )

    fun batchDeleteResponseToModel(r: OssBatchDeleteResponse): OssBatchDeleteModel = OssBatchDeleteModel(
        fileIds = r.fileIdList.orEmpty().map { wireFileIdToLong(it) },
        successCount = r.successCount ?: 0,
        failCount = r.failCount ?: 0,
        message = r.message.orEmpty(),
    )

    fun fileContentUpdateResponseToModel(r: OssFileContentUpdateResponse): OssFileContentUpdateModel =
        OssFileContentUpdateModel(
            fileId = r.fileId.orEmpty(),
            originFileName = r.originFileName.orEmpty(),
            url = r.url.orEmpty(),
            updated = r.updated == true,
            message = r.message.orEmpty(),
        )
}
