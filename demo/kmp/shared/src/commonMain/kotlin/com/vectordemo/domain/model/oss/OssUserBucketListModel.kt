package com.vectordemo.domain.model.oss

import com.vectordemo.domain.dto.http.response.OssUserBucketListResponse
import io.ktor.http.Parameters
import io.ktor.http.parameters

data class OssUserBucketListModel(
    val userId: Long,
    val bucketNames: List<String>,
) {
    companion object {
        fun formParameters(userId: String): Parameters = parameters { append("userId", userId) }

        fun fromResponse(response: OssUserBucketListResponse): OssUserBucketListModel {
            val uid = response.userId?.toLongOrNull() ?: 0L
            return OssUserBucketListModel(userId = uid, bucketNames = response.bucketNameList.orEmpty())
        }
    }
}
