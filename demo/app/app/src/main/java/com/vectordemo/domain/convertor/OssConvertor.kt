package com.vectordemo.domain.convertor

import com.vectordemo.domain.dto.http.response.OssBatchDeleteResponse
import com.vectordemo.domain.dto.http.response.OssBatchUploadResponse
import com.vectordemo.domain.dto.http.response.OssFileContentUpdateResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileIdsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemListResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemRow
import com.vectordemo.domain.dto.http.response.OssUserBucketFileUrlsResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketListResponse
import com.vectordemo.domain.entity.OssUserBucketFileEntity
import com.vectordemo.domain.model.oss.OssBatchDeleteModel
import com.vectordemo.domain.model.oss.OssBatchUploadModel
import com.vectordemo.domain.model.oss.OssBucketFileIdListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemModel
import com.vectordemo.domain.model.oss.OssBucketFileUrlListModel
import com.vectordemo.domain.model.oss.OssFileContentUpdateModel
import com.vectordemo.domain.model.oss.OssUploadItemModel
import com.vectordemo.domain.model.oss.OssUserBucketListModel

object OssConvertor {

    private fun wireFileIdToLong(raw: String?): Long =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L

    fun bucketListResponseToModel(r: OssUserBucketListResponse): OssUserBucketListModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        return OssUserBucketListModel(userId = uid, bucketNames = r.bucketNameList.orEmpty())
    }

    fun fileIdsResponseToModel(r: OssUserBucketFileIdsResponse): OssBucketFileIdListModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        return OssBucketFileIdListModel(
            userId = uid,
            bucketName = r.bucketName.orEmpty(),
            fileIds = r.fileIdList.orEmpty().map { wireFileIdToLong(it) }
        )
    }

    fun fileUrlsResponseToModel(r: OssUserBucketFileUrlsResponse): OssBucketFileUrlListModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        return OssBucketFileUrlListModel(
            userId = uid,
            bucketName = r.bucketName.orEmpty(),
            urls = r.urlList.orEmpty()
        )
    }

    fun fileItemListResponseToModel(r: OssUserBucketFileItemListResponse): OssBucketFileItemListModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        val items = r.items.orEmpty().map { rowToItemModel(it) }
        return OssBucketFileItemListModel(userId = uid, bucketName = r.bucketName.orEmpty(), items = items)
    }

    fun rowToItemModel(row: OssUserBucketFileItemRow): OssBucketFileItemModel = OssBucketFileItemModel(
        fileId = wireFileIdToLong(row.fileId),
        originFileName = row.originFileName.orEmpty(),
        url = row.url.orEmpty()
    )

    fun itemModelToEntity(
        model: OssBucketFileItemModel,
        userId: Long,
        bucketName: String,
        syncedAt: Long
    ): OssUserBucketFileEntity = OssUserBucketFileEntity(
        userId = userId,
        bucketName = bucketName,
        fileId = model.fileId.toString(),
        fileName = model.originFileName,
        fileUrl = model.url,
        syncedAt = syncedAt
    )

    fun itemListModelToEntities(model: OssBucketFileItemListModel, syncedAt: Long): List<OssUserBucketFileEntity> =
        model.items.map { itemModelToEntity(it, model.userId, model.bucketName, syncedAt) }

    fun entityToItemModel(entity: OssUserBucketFileEntity): OssBucketFileItemModel = OssBucketFileItemModel(
        fileId = wireFileIdToLong(entity.fileId),
        originFileName = entity.fileName,
        url = entity.fileUrl
    )

    fun idListModelToEntities(model: OssBucketFileIdListModel, syncedAt: Long): List<OssUserBucketFileEntity> =
        model.fileIds.map { fid ->
            OssUserBucketFileEntity(
                userId = model.userId,
                bucketName = model.bucketName,
                fileId = fid.toString(),
                fileName = "",
                fileUrl = "",
                syncedAt = syncedAt
            )
        }

    fun batchUploadResponseToModel(r: OssBatchUploadResponse): OssBatchUploadModel {
        val uid = r.userId?.toLongOrNull() ?: 0L
        val items = r.items.orEmpty().map {
            OssUploadItemModel(
                originFileName = it.originFileName.orEmpty(),
                success = it.success,
                duplicated = it.duplicated,
                fileId = it.fileId.orEmpty(),
                url = it.url.orEmpty(),
                message = it.message.orEmpty()
            )
        }
        return OssBatchUploadModel(
            userId = uid,
            bucketName = r.bucketName.orEmpty(),
            successCount = r.successCount ?: 0,
            failCount = r.failCount ?: 0,
            items = items
        )
    }

    fun batchDeleteResponseToModel(r: OssBatchDeleteResponse): OssBatchDeleteModel = OssBatchDeleteModel(
        fileIds = r.fileIdList.orEmpty().map { wireFileIdToLong(it) },
        successCount = r.successCount ?: 0,
        failCount = r.failCount ?: 0,
        message = r.message.orEmpty()
    )

    fun fileContentUpdateResponseToModel(r: OssFileContentUpdateResponse): OssFileContentUpdateModel =
        OssFileContentUpdateModel(
            fileId = r.fileId.orEmpty(),
            originFileName = r.originFileName.orEmpty(),
            url = r.url.orEmpty(),
            updated = r.updated == true,
            message = r.message.orEmpty()
        )
}
