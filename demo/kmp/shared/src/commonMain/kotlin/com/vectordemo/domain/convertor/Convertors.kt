package com.vectordemo.domain.convertor

import com.vectordemo.domain.model.OssBucketFileItemListModel
import com.vectordemo.domain.model.OssBucketFileItemModel
import com.vectordemo.domain.model.OssUserBucketListModel
import com.vectordemo.domain.model.UserSessionModel
import com.vectordemo.domain.platform.currentTimeMillis
import com.vectordemo.repository.api.OssBatchDeleteResponse
import com.vectordemo.repository.api.OssBatchUploadResponse
import com.vectordemo.repository.api.OssFileContentUpdateResponse
import com.vectordemo.repository.api.OssUploadItemResult
import com.vectordemo.repository.api.OssUserBucketFileItemListResponse
import com.vectordemo.repository.api.OssUserBucketFileItemRow
import com.vectordemo.repository.api.OssUserBucketListResponse
import com.vectordemo.repository.api.UserAuthResponse

object UserConvertor {
    fun authResponseToSessionModel(auth: UserAuthResponse, password: String): UserSessionModel {
        val uid = auth.userId?.toLongOrNull() ?: 0L
        return UserSessionModel(
            userId = uid,
            account = auth.account.orEmpty(),
            name = auth.name.orEmpty(),
            avatarUrl = auth.avatarUrl.orEmpty(),
            accessToken = auth.accessToken.orEmpty(),
            password = password,
            isCurrent = true,
            lastLoginAt = currentTimeMillis(),
        )
    }
}

data class OssUploadItemModel(
    val originFileName: String,
    val success: Boolean,
    val duplicated: Boolean,
    val fileId: String,
    val url: String,
    val message: String,
)

data class OssBatchUploadModel(
    val userId: Long,
    val bucketName: String,
    val successCount: Int,
    val failCount: Int,
    val items: List<OssUploadItemModel>,
)

data class OssBatchDeleteModel(
    val fileIds: List<Long>,
    val successCount: Int,
    val failCount: Int,
    val message: String,
)

data class OssFileContentUpdateModel(
    val fileId: String,
    val originFileName: String,
    val url: String,
    val updated: Boolean,
    val message: String,
)

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

    private fun itemToModel(it: OssUploadItemResult): OssUploadItemModel {
        return OssUploadItemModel(
            originFileName = it.originFileName.orEmpty(),
            success = it.success,
            duplicated = it.duplicated,
            fileId = it.fileId.orEmpty(),
            url = it.url.orEmpty(),
            message = it.message.orEmpty(),
        )
    }

    fun batchDeleteResponseToModel(r: OssBatchDeleteResponse): OssBatchDeleteModel = OssBatchDeleteModel(
        fileIds = r.fileIdList.orEmpty().map { wireFileIdToLong(it) },
        successCount = r.successCount ?: 0,
        failCount = r.failCount ?: 0,
        message = r.message.orEmpty(),
    )

    fun fileContentUpdateResponseToModel(r: OssFileContentUpdateResponse): OssFileContentUpdateModel {
        return OssFileContentUpdateModel(
            fileId = r.fileId.orEmpty(),
            originFileName = r.originFileName.orEmpty(),
            url = r.url.orEmpty(),
            updated = r.updated == true,
            message = r.message.orEmpty(),
        )
    }
}
