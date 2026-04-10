package com.vectordemo.manager.user

import android.content.Context
import com.vectordemo.dataSource.local.UserLocalSource
import com.vectordemo.domain.convertor.UserConvertor
import com.vectordemo.domain.model.user.UserSessionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserManager private constructor(context: Context) {
    private val userLocalSource: UserLocalSource = UserLocalSource.getInstance(context)
    @Volatile
    private var currentUserSessionCache: UserSessionModel? = null

    suspend fun saveCurrentUser(session: UserSessionModel) = withContext(Dispatchers.IO) {
        if (isTouristSession(session)) {
            currentUserSessionCache = session.copy(isCurrent = true, lastLoginAt = System.currentTimeMillis())
            return@withContext
        }
        val currentSession = session.copy(isCurrent = true, lastLoginAt = System.currentTimeMillis())
        userLocalSource.saveCurrentUser(UserConvertor.model2Entity(currentSession))
        currentUserSessionCache = currentSession
    }

    suspend fun getCurrentUser(): UserSessionModel? = withContext(Dispatchers.IO) {
        val cached = currentUserSessionCache
        if (cached != null && cached.accessToken.isNotBlank()) return@withContext cached
        userLocalSource.getCurrentUser()?.let { UserConvertor.entity2Model(it) }?.also { currentUserSessionCache = it }
    }

    suspend fun getAllUsers(): List<UserSessionModel> = withContext(Dispatchers.IO) {
        userLocalSource.getAllUsers()
            .map { UserConvertor.entity2Model(it) }
            .filterNot { isTouristSession(it) }
    }

    private fun isTouristSession(session: UserSessionModel): Boolean {
        return session.userId == 1L || session.account == "tourist" || session.accessToken == "tourist"
    }

    suspend fun clearCurrentUser() = withContext(Dispatchers.IO) {
        userLocalSource.clearCurrentUser()
        currentUserSessionCache = null
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
