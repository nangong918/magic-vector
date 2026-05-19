package com.vectordemo.manager.user

import com.vectordemo.dataSource.local.UserLocalSource
import com.vectordemo.domain.model.user.UserSessionModel
import com.vectordemo.domain.platform.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class UserManager(
    private val userLocalSource: UserLocalSource,
) {
    private var currentUserSessionCache: UserSessionModel? = null

    suspend fun saveCurrentUser(session: UserSessionModel) = withContext(Dispatchers.IO) {
        if (isTouristSession(session)) {
            currentUserSessionCache = session.copy(isCurrent = true, lastLoginAt = currentTimeMillis())
            return@withContext
        }
        val currentSession = session.copy(isCurrent = true, lastLoginAt = currentTimeMillis())
        userLocalSource.saveCurrentUser(currentSession)
        currentUserSessionCache = currentSession
    }

    /** 同步读取内存缓存（供 HTTP 鉴权头注入，对齐 demo/app AuthInterceptor 读当前用户）。 */
    fun peekCurrentUser(): UserSessionModel? = currentUserSessionCache

    suspend fun getCurrentUser(): UserSessionModel? = withContext(Dispatchers.IO) {
        val cached = currentUserSessionCache
        if (cached != null && cached.accessToken.isNotBlank()) return@withContext cached
        userLocalSource.getCurrentUser()?.also { currentUserSessionCache = it }
    }

    suspend fun getAllUsers(): List<UserSessionModel> = withContext(Dispatchers.IO) {
        userLocalSource.getAllUsers().filterNot { isTouristSession(it) }
    }

    suspend fun clearCurrentUser() = withContext(Dispatchers.IO) {
        userLocalSource.clearCurrentUser()
        currentUserSessionCache = null
    }

    private fun isTouristSession(session: UserSessionModel): Boolean {
        return session.userId == 1L || session.account == "tourist" || session.accessToken == "tourist"
    }
}
