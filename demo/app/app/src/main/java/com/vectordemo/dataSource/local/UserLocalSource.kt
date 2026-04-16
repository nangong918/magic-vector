package com.vectordemo.dataSource.local

import android.content.Context
import com.vectordemo.dataSource.local.db.VectorDatabase
import com.vectordemo.domain.convertor.UserConvertor
import com.vectordemo.domain.model.user.UserSessionModel
import com.vectordemo.repository.dao.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户本地数据源：对外仅 [UserSessionModel]，内部用 Entity + [UserConvertor]。
 */
class UserLocalSource private constructor(context: Context) {
    private val userDao: UserDao = VectorDatabase.getInstance(context).userDao()

    suspend fun saveCurrentUser(session: UserSessionModel) = withContext(Dispatchers.IO) {
        userDao.clearCurrentFlag()
        userDao.upsert(UserConvertor.model2Entity(session))
    }

    suspend fun getCurrentUser(): UserSessionModel? = withContext(Dispatchers.IO) {
        userDao.getCurrent()?.let(UserConvertor::entity2Model)
    }

    suspend fun getAllUsers(): List<UserSessionModel> = withContext(Dispatchers.IO) {
        userDao.getAll().map(UserConvertor::entity2Model)
    }

    suspend fun clearCurrentUser() = withContext(Dispatchers.IO) {
        userDao.getCurrent()?.let { entity ->
            userDao.upsert(entity.copy(accessToken = "", isCurrent = false))
        }
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
