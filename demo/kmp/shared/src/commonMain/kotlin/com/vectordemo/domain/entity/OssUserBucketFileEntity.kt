package com.vectordemo.domain.entity

data class OssUserBucketFileEntity(
    val userId: Long,
    val bucketName: String,
    val fileId: String,
    val fileName: String,
    val fileUrl: String,
    val syncedAt: Long = 0L,
) {
    companion object {
        const val EMPTY_BUCKET_FILE_ID: String = "__vdb_bucket_placeholder__"
    }
}
