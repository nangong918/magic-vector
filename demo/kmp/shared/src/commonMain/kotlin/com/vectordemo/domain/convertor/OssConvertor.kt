package com.vectordemo.domain.convertor

import com.vectordemo.domain.entity.OssUserBucketFileEntity
import com.vectordemo.domain.model.oss.OssBucketFileItemListModel
import com.vectordemo.domain.model.oss.OssBucketFileItemModel

object OssConvertor {
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

    fun entityToItemModel(entity: OssUserBucketFileEntity): OssBucketFileItemModel {
        val fileId = entity.fileId.trim().takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L
        return OssBucketFileItemModel(
            fileId = fileId,
            originFileName = entity.fileName,
            url = entity.fileUrl,
        )
    }
}
