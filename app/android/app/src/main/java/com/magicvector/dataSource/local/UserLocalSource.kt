package com.magicvector.dataSource.local

import android.content.Context
import com.magicvector.dataSource.local.db.VectorDatabase
import com.magicvector.domain.entity.UserEntity
import com.magicvector.repository.dao.UserDao

class UserLocalSource private constructor(
    context: Context
) {
    private val userDao: UserDao = VectorDatabase.getInstance(context).userDao()

    suspend fun saveCurrentUser(
        userEntity: UserEntity,
        handleSaveCurrentUser: (UserEntity?) -> Unit
    ) {
        userDao.clearCurrentFlag()
        userDao.upsert(userEntity)
        handleSaveCurrentUser(userEntity)
    }

    suspend fun getCurrentUser(
        handleCurrentUser: (UserEntity?) -> Unit
    ): UserEntity? {
        val current = userDao.getCurrent()
        handleCurrentUser(current)
        return current
    }

    suspend fun getAllUsers(
        handleAllUsers: (List<UserEntity>) -> Unit
    ): List<UserEntity> {
        val users = userDao.getAll()
        handleAllUsers(users)
        return users
    }

    suspend fun clearCurrentUser(
        handleClearCurrentUser: (UserEntity?) -> Unit
    ) {
        val current = userDao.getCurrent()
        if (current == null) {
            handleClearCurrentUser(null)
            return
        }
        val cleared = current.copy(
            accessToken = "",
            isCurrent = false
        )
        userDao.upsert(cleared)
        handleClearCurrentUser(cleared)
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
