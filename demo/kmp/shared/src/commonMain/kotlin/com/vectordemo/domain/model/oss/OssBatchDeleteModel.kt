package com.vectordemo.domain.model.oss

import com.vectordemo.domain.dto.http.response.OssBatchDeleteResponse
import io.ktor.http.Parameters
import io.ktor.http.parameters

data class OssBatchDeleteModel(
    val fileIds: List<Long>,
    val successCount: Int,
    val failCount: Int,
    val message: String,
) {
    companion object {
        fun formParameters(fileIds: List<String>): Parameters = parameters {
            fileIds.forEach { append("fileIdList", it) }
        }

        fun fromResponse(response: OssBatchDeleteResponse): OssBatchDeleteModel = OssBatchDeleteModel(
            fileIds = response.fileIdList.orEmpty().map { wireFileIdToLong(it) },
            successCount = response.successCount ?: 0,
            failCount = response.failCount ?: 0,
            message = response.message.orEmpty(),
        )

        private fun wireFileIdToLong(raw: String?): Long =
            raw?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L
    }
}
