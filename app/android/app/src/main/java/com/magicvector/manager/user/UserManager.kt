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

    suspend fun saveCurrentUser(session: UserSession) {
        userDao.upsert(
            UserEntity(
                id = UserEntity.CURRENT_ROW_ID,
                userId = session.userId,
                account = session.account,
                name = session.name,
                avatarUrl = session.avatarUrl,
                accessToken = session.accessToken
            )
        )
    }

    suspend fun getCurrentUser(): UserSession? {
        return userDao.getById()?.toSession()
    }

    suspend fun clearCurrentUser() {
        userDao.deleteById()
    }

    private fun UserEntity.toSession(): UserSession {
        return UserSession(
            userId = userId,
            account = account,
            name = name,
            avatarUrl = avatarUrl,
            accessToken = accessToken
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
