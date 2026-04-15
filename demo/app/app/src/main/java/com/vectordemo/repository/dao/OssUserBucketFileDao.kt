package com.vectordemo.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vectordemo.domain.entity.OssUserBucketFileEntity

@Dao
interface OssUserBucketFileDao {
    @Query(
        "SELECT DISTINCT bucket_name FROM oss_user_bucket_file WHERE user_id = :userId ORDER BY bucket_name ASC"
    )
    suspend fun distinctBuckets(userId: Long): List<String>

    @Query(
        "SELECT * FROM oss_user_bucket_file WHERE user_id = :userId AND bucket_name = :bucketName ORDER BY file_id ASC"
    )
    suspend fun rowsInBucket(userId: Long, bucketName: String): List<OssUserBucketFileEntity>

    @Query(
        "SELECT COUNT(*) FROM oss_user_bucket_file WHERE user_id = :userId AND bucket_name = :bucketName AND file_id != :placeholder"
    )
    suspend fun countRealFiles(userId: Long, bucketName: String, placeholder: String): Int

    @Query("DELETE FROM oss_user_bucket_file WHERE user_id = :userId AND bucket_name = :bucketName")
    suspend fun deleteAllForUserBucket(userId: Long, bucketName: String)

    @Query(
        "DELETE FROM oss_user_bucket_file WHERE user_id = :userId AND bucket_name = :bucketName AND file_id = :fileId"
    )
    suspend fun deleteRow(userId: Long, bucketName: String, fileId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<OssUserBucketFileEntity>)
}
