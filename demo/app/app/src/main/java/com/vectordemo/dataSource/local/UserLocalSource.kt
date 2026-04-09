package com.vectordemo.dataSource.local

import android.content.Context
import com.vectordemo.dataSource.local.db.VectorDatabase
import com.vectordemo.domain.entity.UserEntity
import com.vectordemo.repository.dao.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserLocalSource private constructor(context: Context) {
    private val userDao: UserDao = VectorDatabase.getInstance(context).userDao()

    suspend fun saveCurrentUser(userEntity: UserEntity) = withContext(Dispatchers.IO) {
        userDao.clearCurrentFlag()
        userDao.upsert(userEntity)
    }

    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) { userDao.getCurrent() }
    suspend fun getAllUsers(): List<UserEntity> = withContext(Dispatchers.IO) { userDao.getAll() }

    suspend fun clearCurrentUser() = withContext(Dispatchers.IO) {
        userDao.getCurrent()?.let { userDao.upsert(it.copy(accessToken = "", isCurrent = false)) }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserLocalSource? = null
        fun getInstance(context: Context): UserLocalSource {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserLocalSource(context).also { INSTANCE = it }
            }
        }
    }
}
