package com.vectordemo.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * 单表：用户 → 多个 bucket；每个 bucket → 多条文件（fileId、fileName、fileUrl）。
 * 当仅有桶名、尚无文件缓存时，使用 [EMPTY_BUCKET_FILE_ID] 作为占位 [fileId]。
 */
@Entity(
    tableName = "oss_user_bucket_file",
    primaryKeys = ["user_id", "bucket_name", "file_id"],
    indices = [Index(value = ["user_id"], name = "idx_oss_ubf_user")]
)
data class OssUserBucketFileEntity(
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    @ColumnInfo(name = "file_id") val fileId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_url") val fileUrl: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = 0L
) {
    companion object {
        const val EMPTY_BUCKET_FILE_ID: String = "__vdb_bucket_placeholder__"
    }
}
