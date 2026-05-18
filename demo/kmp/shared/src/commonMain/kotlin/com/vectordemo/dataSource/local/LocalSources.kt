package com.vectordemo.dataSource.local

import com.vectordemo.domain.model.OssBucketFileItemModel
import com.vectordemo.domain.model.OssBucketFileItemListModel
import com.vectordemo.domain.model.OssUserBucketListModel
import com.vectordemo.domain.model.UserSessionModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

class UserLocalSource {
    private val sessions = MutableStateFlow<List<UserSessionModel>>(emptyList())

    suspend fun saveCurrentUser(session: UserSessionModel) {
        sessions.update { old ->
            val next = old.filterNot { it.account == session.account }.map { it.copy(isCurrent = false) }
            next + session.copy(isCurrent = true)
        }
    }

    suspend fun getCurrentUser(): UserSessionModel? {
        return sessions.first().lastOrNull { it.isCurrent }
    }

    suspend fun getAllUsers(): List<UserSessionModel> = sessions.first().sortedByDescending { it.lastLoginAt }

    suspend fun clearCurrentUser() {
        sessions.update { list -> list.map { it.copy(isCurrent = false, accessToken = "") } }
    }
}

class OssLocalSource {
    private val bucketsByUser = MutableStateFlow<Map<Long, List<String>>>(emptyMap())
    private val filesByBucket = MutableStateFlow<Map<Pair<Long, String>, List<OssBucketFileItemModel>>>(emptyMap())

    suspend fun syncBucketList(model: OssUserBucketListModel) {
        bucketsByUser.update { it + (model.userId to model.bucketNames) }
    }

    suspend fun syncBucketFiles(model: OssBucketFileItemListModel) {
        filesByBucket.update { it + ((model.userId to model.bucketName) to model.items) }
    }

    suspend fun getCachedBucketNames(userId: Long): List<String> = bucketsByUser.first()[userId].orEmpty()

    suspend fun getCachedBucketFiles(userId: Long, bucketName: String): List<OssBucketFileItemModel> {
        return filesByBucket.first()[userId to bucketName].orEmpty()
    }

    suspend fun deleteCachedFile(userId: Long, bucketName: String, fileId: Long) {
        filesByBucket.update { old ->
            val key = userId to bucketName
            val current = old[key].orEmpty()
            old + (key to current.filterNot { it.fileId == fileId })
        }
    }
}
