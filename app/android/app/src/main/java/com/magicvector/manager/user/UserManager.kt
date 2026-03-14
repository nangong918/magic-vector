package com.magicvector.manager.user

import android.content.Context
import com.magicvector.manager.db.VectorDatabase

/**
 * UserManager 负责用户会话持久化。
 * 这里采用 Room 存储当前登录用户，避免进程重启后状态丢失。
 */
class UserManager private constructor(
    context: Context
) {
    private val userDao: UserDao = VectorDatabase.getInstance(context).userDao()
    @Volatile
    private var currentUserSessionCache: UserSession? = null

    suspend fun saveCurrentUser(session: UserSession) {
        val loginAt = System.currentTimeMillis()
        userDao.clearCurrentFlag()
        userDao.upsert(
            UserEntity(
                userId = session.userId,
                account = session.account,
                name = session.name,
                avatarUrl = session.avatarUrl,
                accessToken = session.accessToken,
                password = session.password,
                isCurrent = true,
                lastLoginAt = loginAt
            )
        )
        currentUserSessionCache = session.copy(
            isCurrent = true,
            lastLoginAt = loginAt
        )
    }

    suspend fun getCurrentUser(): UserSession? {
        val cached = currentUserSessionCache
        if (cached != null && cached.accessToken.isNotBlank()) {
            return cached
        }
        val current = userDao.getCurrent()?.toSession()
        currentUserSessionCache = current
        return current
    }

    suspend fun getAllUsers(): List<UserSession> {
        return userDao.getAll().map { it.toSession() }
    }

    suspend fun clearCurrentUser() {
        val current = userDao.getCurrent() ?: return
        userDao.upsert(
            current.copy(
                accessToken = "",
                isCurrent = false
            )
        )
        currentUserSessionCache = null
    }

    private fun UserEntity.toSession(): UserSession {
        return UserSession(
            userId = userId,
            account = account,
            name = name,
            avatarUrl = avatarUrl,
            accessToken = accessToken,
            password = password,
            isCurrent = isCurrent,
            lastLoginAt = lastLoginAt
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: UserManager? = null

        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserManager(context).also { INSTANCE = it }
            }
        }
    }
}
