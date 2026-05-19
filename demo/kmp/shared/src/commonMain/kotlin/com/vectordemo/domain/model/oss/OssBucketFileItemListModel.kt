package com.vectordemo.domain.model.oss

import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemListResponse
import com.vectordemo.domain.dto.http.response.OssUserBucketFileItemRow
import io.ktor.http.Parameters
import io.ktor.http.parameters

data class OssBucketFileItemListModel(
    val userId: Long,
    val bucketName: String,
    val items: List<OssBucketFileItemModel>,
) {
    companion object {
        fun formParameters(userId: String, bucketName: String): Parameters = parameters {
            append("userId", userId)
            append("bucketName", bucketName)
        }

        fun fromResponse(response: OssUserBucketFileItemListResponse): OssBucketFileItemListModel {
            val uid = response.userId?.toLongOrNull() ?: 0L
            val items = response.items.orEmpty().map { rowToItem(it) }
            return OssBucketFileItemListModel(
                userId = uid,
                bucketName = response.bucketName.orEmpty(),
                items = items,
            )
        }

        private fun wireFileIdToLong(raw: String?): Long =
            raw?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L

        fun rowToItem(row: OssUserBucketFileItemRow): OssBucketFileItemModel = OssBucketFileItemModel(
            fileId = wireFileIdToLong(row.fileId),
            originFileName = row.originFileName.orEmpty(),
            url = row.url.orEmpty(),
        )
    }
}
