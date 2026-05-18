package com.vectordemo.manager

import com.vectordemo.dataSource.local.OssLocalSource
import com.vectordemo.dataSource.local.UserLocalSource
import com.vectordemo.dataSource.remote.OssRemoteApiSource
import com.vectordemo.domain.convertor.OssBatchDeleteModel
import com.vectordemo.domain.convertor.OssBatchUploadModel
import com.vectordemo.domain.convertor.OssFileContentUpdateModel
import com.vectordemo.domain.model.OssBucketFileItemListModel
import com.vectordemo.domain.model.OssBucketFileItemModel
import com.vectordemo.domain.model.OssUserBucketListModel
import com.vectordemo.domain.model.UserSessionModel
import com.vectordemo.repository.api.MultipartPartPayload

class UserManager(private val userLocalSource: UserLocalSource) {
    private var currentUserSessionCache: UserSessionModel? = null

    suspend fun saveCurrentUser(session: UserSessionModel) {
        if (isTouristSession(session)) {
            currentUserSessionCache = session.copy(isCurrent = true)
            return
        }
        val currentSession = session.copy(isCurrent = true)
        userLocalSource.saveCurrentUser(currentSession)
        currentUserSessionCache = currentSession
    }

    suspend fun getCurrentUser(): UserSessionModel? {
        val cached = currentUserSessionCache
        if (cached != null && cached.accessToken.isNotBlank()) return cached
        return userLocalSource.getCurrentUser()?.also { currentUserSessionCache = it }
    }

    suspend fun getAllUsers(): List<UserSessionModel> {
        return userLocalSource.getAllUsers().filterNot { isTouristSession(it) }
    }

    suspend fun clearCurrentUser() {
        userLocalSource.clearCurrentUser()
        currentUserSessionCache = null
    }

    private fun isTouristSession(session: UserSessionModel): Boolean {
        return session.userId == 1L || session.account == "tourist" || session.accessToken == "tourist"
    }
}

class OssManager(
    private val remote: OssRemoteApiSource,
    private val local: OssLocalSource,
) {
    suspend fun syncUserBucketList(userId: Long): OssUserBucketListModel {
        val model = remote.ossUserBucketList(userId.toString())
        if (model.userId > 0L) local.syncBucketList(model)
        return model
    }

    suspend fun syncBucketFileItemList(userId: Long, bucketName: String): OssBucketFileItemListModel {
        val model = remote.ossUserBucketFileItemList(userId.toString(), bucketName)
        if (model.userId > 0L && model.bucketName.isNotBlank()) local.syncBucketFiles(model)
        return model
    }

    suspend fun getCachedUserBucketList(userId: Long): OssUserBucketListModel? {
        val names = local.getCachedBucketNames(userId)
        return if (names.isEmpty()) null else OssUserBucketListModel(userId = userId, bucketNames = names)
    }

    suspend fun getCachedBucketFileItems(userId: Long, bucketName: String): List<OssBucketFileItemModel> {
        return local.getCachedBucketFiles(userId, bucketName)
    }

    suspend fun batchUploadSingle(
        userId: Long,
        bucketName: String?,
        file: MultipartPartPayload,
    ): OssBatchUploadModel {
        return remote.ossBatchUploadSingle(userId.toString(), bucketName, file)
    }

    suspend fun batchDelete(fileIds: List<Long>, userId: Long, bucketName: String): OssBatchDeleteModel {
        val model = remote.ossBatchDelete(fileIds.map { it.toString() })
        fileIds.forEach { local.deleteCachedFile(userId, bucketName, it) }
        return model
    }

    suspend fun updateFileContent(fileId: String, file: MultipartPartPayload): OssFileContentUpdateModel {
        return remote.ossUpdateFileContent(fileId, file)
    }
}
