package com.magicvector.manager.user

import android.content.Context
import com.magicvector.dataSource.local.UserLocalSource
import com.magicvector.domain.convertor.UserConvertor
import com.magicvector.domain.model.UserSessionModel

/**
 * UserManager 负责用户会话持久化。
 * 这里采用 Room 存储当前登录用户，避免进程重启后状态丢失。
 */
class UserManager private constructor(
    context: Context
) {
    private val userLocalSource: UserLocalSource = UserLocalSource.getInstance(context)
    @Volatile
    private var currentUserSessionCache: UserSessionModel? = null

    suspend fun saveCurrentUser(session: UserSessionModel) {
        val loginAt = System.currentTimeMillis()
        val currentSession = session.copy(
            isCurrent = true,
            lastLoginAt = loginAt
        )
        val userEntity = UserConvertor.model2Entity(currentSession)
        userLocalSource.saveCurrentUser(userEntity) { savedEntity ->
            if (savedEntity != null) {
                currentUserSessionCache = currentSession.copy(
                    isCurrent = true,
                    lastLoginAt = savedEntity.lastLoginAt
                )
            }
        }
    }

    suspend fun getCurrentUser(): UserSessionModel? {
        val cached = currentUserSessionCache
        if (cached != null && cached.accessToken.isNotBlank()) {
            return cached
        }
        val current = userLocalSource.getCurrentUser { entity ->
            currentUserSessionCache = entity?.let { UserConvertor.entity2Model(it) }
        }?.let { UserConvertor.entity2Model(it) }
        return current
    }

    suspend fun getAllUsers(): List<UserSessionModel> {
        val entities = userLocalSource.getAllUsers { }
        return entities.map { UserConvertor.entity2Model(it) }
    }

    suspend fun clearCurrentUser() {
        userLocalSource.clearCurrentUser { _ ->
            currentUserSessionCache = null
        }
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
